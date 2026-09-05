package com.xypu.service;

import com.alibaba.fastjson.JSON;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.po.VideoInfoFile;
import com.xypu.entity.vo.UploadTaskVO;
import com.xypu.utils.RedisUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * 视频异步转码服务
 * 必须独立为 Spring Bean（不能与调用方在同一 Bean），@Async 代理才能生效
 */
@Service
public class VideoTranscodeService {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodeService.class);

    @Value("${project.folder}")
    private String projectFolder;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 异步合并分片并转码为 HLS 格式
     * 由 FileController 在最后一片上传完成后触发，不阻塞 HTTP 响应
     */
    @Async
    public void mergeAndTranscode(String uploadId, String videoId, String fileName, Integer totalChunks) {
        File tempDir = new File(projectFolder + "temp/" + uploadId + "/");
        File mergedFile = new File(tempDir, "merged.tmp");
        File outputDir = new File(projectFolder + "video/" + videoId + "/");

        try {
            // 按 chunkIndex 顺序合并所有分片为一个临时文件
            try (OutputStream out = Files.newOutputStream(mergedFile.toPath(), StandardOpenOption.CREATE)) {
                for (int i = 0; i < totalChunks; i++) {
                    File chunk = new File(tempDir, String.valueOf(i));
                    if (!chunk.exists()) {
                        log.error("分片缺失，uploadId={}, chunkIndex={}", uploadId, i);
                        return;
                    }
                    Files.copy(chunk.toPath(), out);
                }
            }

            // 创建 HLS 输出目录
            outputDir.mkdirs();

            // 调用 FFmpeg 将合并文件转码为 HLS 格式（m3u8 + ts 分片）
            String m3u8Path = outputDir.getAbsolutePath() + "/index.m3u8";
            String tsPattern = outputDir.getAbsolutePath() + "/%d.ts";
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-y",
                    "-i", mergedFile.getAbsolutePath(),
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_segment_filename", tsPattern,
                    m3u8Path
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取 FFmpeg 输出防止进程阻塞
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[FFmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 转码失败，exitCode={}, videoId={}", exitCode, videoId);
                return;
            }

            // 转码完成后检查用户是否已取消上传（VideoInfo 已被删除）
            if (videoInfoService.getVideoById(videoId) == null) {
                log.info("视频已被取消，清理转码输出目录，videoId={}", videoId);
                FileUtils.deleteQuietly(outputDir);
                redisUtils.sRem(RedisUtils.VIDEO_DELETE_QUEUE, videoId);
                return;
            }

            // 解析 m3u8 文件计算视频总时长（累加所有 #EXTINF 值）
            int totalDuration = parseDurationFromM3u8(new File(m3u8Path));

            // 保存视频文件信息到 DB
            VideoInfoFile videoInfoFile = new VideoInfoFile();
            videoInfoFile.setFileId(RandomStringUtils.randomAlphanumeric(20));
            videoInfoFile.setUserId(getVideoUserId(videoId));
            videoInfoFile.setVideoId(videoId);
            videoInfoFile.setFileName(fileName);
            videoInfoFile.setFileIndex(0);
            videoInfoFile.setFileSize(mergedFile.length());
            videoInfoFile.setFilePath("video/" + videoId + "/");
            videoInfoFile.setDuration(totalDuration);
            videoInfoFileService.save(videoInfoFile);

            // 更新 VideoInfo 的时长字段（auditStatus 保持 0，等待用户提交视频信息）
            VideoInfo update = new VideoInfo();
            update.setVideoId(videoId);
            update.setDuration((long) totalDuration);
            update.setLastUpdateTime(new Date());
            videoInfoService.updateById(update);

            log.info("转码完成，videoId={}, duration={}s", videoId, totalDuration);
        } catch (Exception e) {
            log.error("转码异常，videoId={}", videoId, e);
        } finally {
            // 转码完成后删除临时目录释放磁盘空间
            FileUtils.deleteQuietly(tempDir);
            // 清理 Redis 上传任务
            redisUtils.delete(RedisUtils.UPLOAD_TASK_PREFIX + uploadId);
        }
    }

    /** 解析 m3u8 文件，累加所有 #EXTINF 行的时长值，返回总秒数（向上取整） */
    private int parseDurationFromM3u8(File m3u8File) throws IOException {
        double total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#EXTINF:")) {
                    String durStr = line.substring(8, line.indexOf(','));
                    total += Double.parseDouble(durStr);
                }
            }
        }
        return (int) Math.ceil(total);
    }

    private String getVideoUserId(String videoId) {
        VideoInfo info = videoInfoService.getVideoById(videoId);
        return info != null ? info.getUserId() : "";
    }
}
