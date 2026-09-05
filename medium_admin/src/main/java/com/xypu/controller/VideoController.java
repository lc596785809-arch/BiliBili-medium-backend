package com.xypu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xypu.entity.dto.VideoAuditDTO;
import com.xypu.entity.dto.VideoQueryDTO;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.po.VideoInfoFile;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import com.xypu.service.VideoInfoFileService;
import com.xypu.service.VideoInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/admin/videoInfo")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    @Value("${project.folder}")
    private String projectFolder;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    /**
     * 分页查询视频稿件列表
     * 支持视频名称模糊检索、审核状态筛选、推荐状态筛选
     */
    @PostMapping("/loadVideoList")
    public ResponseVO<IPage<VideoInfo>> loadVideoList(@RequestBody VideoQueryDTO dto) {
        return ResponseVO.ok(videoInfoService.loadVideoList(dto));
    }

    /**
     * 视频审核：通过（auditStatus=2）或驳回（auditStatus=3）
     */
    @PostMapping("/auditVideo")
    public ResponseVO<Void> auditVideo(@RequestBody @Validated VideoAuditDTO dto) {
        videoInfoService.auditVideo(dto);
        return ResponseVO.ok();
    }

    /**
     * 设置视频首页推荐标识
     * recommendType=1 设置推荐并记录推荐时间；recommendType=0 取消推荐
     */
    @PostMapping("/recommendVideo")
    public ResponseVO<Void> recommendVideo(@RequestParam String videoId,
                                           @RequestParam Integer recommendType) {
        videoInfoService.recommendVideo(videoId, recommendType);
        return ResponseVO.ok();
    }

    /**
     * 逻辑删除视频（isDeleted=1）
     * 不物理删除数据库记录与源文件，前台不再展示，后台数据保留
     */
    @PostMapping("/deleteVideo")
    public ResponseVO<Void> deleteVideo(@RequestParam String videoId) {
        videoInfoService.deleteVideo(videoId);
        return ResponseVO.ok();
    }

    /**
     * 统计系统中未删除视频总数，用于后台首页数据看板
     */
    @GetMapping("/getVideoCount")
    public ResponseVO<Long> getVideoCount() {
        return ResponseVO.ok(videoInfoService.getVideoCount());
    }

    /**
     * 下载完整 MP4 视频
     * 服务端调用 FFmpeg 将 HLS（m3u8+ts）重封装为 MP4，以文件流返回给浏览器
     * 下载完成后自动删除临时 MP4 文件，节省磁盘
     */
    @GetMapping("/downloadVideo")
    public void downloadVideo(@RequestParam String videoId, HttpServletResponse response) throws IOException {
        VideoInfo videoInfo = videoInfoService.getVideoById(videoId);
        if (videoInfo == null || videoInfo.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        VideoInfoFile fileInfo = videoInfoFileService.getByVideoId(videoId);
        if (fileInfo == null) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        String m3u8Path = projectFolder + fileInfo.getFilePath() + "index.m3u8";
        String tempMp4Name = videoId + "_download.mp4";
        File tempMp4 = new File(projectFolder + "temp/" + tempMp4Name);
        tempMp4.getParentFile().mkdirs();

        // 使用 FFmpeg 将 m3u8+ts 重封装为 MP4（-c copy 不重新编码，速度快）
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-y",
                "-allowed_extensions", "ALL",
                "-i", m3u8Path,
                "-c", "copy",
                tempMp4.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            // 消费 FFmpeg 输出，防止进程因输出缓冲区满而阻塞
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) { /* 仅消费，不记录 */ }
            }
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCodeEnum.CODE_500);
        }

        if (!tempMp4.exists()) {
            throw new BusinessException(ErrorCodeEnum.CODE_500);
        }

        // 设置下载响应头，文件名 URL 编码处理中文
        String encodedName = URLEncoder.encode(videoInfo.getVideoName() + ".mp4", StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(tempMp4.length());

        try (InputStream in = new FileInputStream(tempMp4);
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        } finally {
            // 下载完成后删除临时 MP4，不持久保存
            if (!tempMp4.delete()) {
                log.warn("临时 MP4 删除失败: {}", tempMp4.getAbsolutePath());
            }
        }
    }

    /**
     * 切换视频公开/私密状态
     * 私密视频前台不可访问，仅后台管理员可见
     */
    @PostMapping("/setVideoPublic")
    public ResponseVO<Void> setVideoPublic(@RequestParam String videoId,
                                           @RequestParam Integer isPublic) {
        videoInfoService.setVideoPublic(videoId, isPublic);
        return ResponseVO.ok();
    }
}
