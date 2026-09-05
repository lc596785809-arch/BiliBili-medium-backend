package com.xypu.controller;

import com.alibaba.fastjson.JSON;
import com.xypu.context.UserContext;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.po.VideoInfoFile;
import com.xypu.entity.vo.PreUploadVO;
import com.xypu.entity.vo.UploadTaskVO;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import com.xypu.service.VideoInfoFileService;
import com.xypu.service.VideoInfoService;
import com.xypu.service.VideoTranscodeService;
import com.xypu.utils.RedisUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client/file")
public class FileController {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
    /** 图片资源浏览器缓存 7 天，减少重复请求 */
    private static final int IMAGE_CACHE_MAX_AGE = 604800;

    @Value("${project.folder}")
    private String projectFolder;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private VideoTranscodeService videoTranscodeService;

    /**
     * 视频预上传：生成 uploadId 和视频 ID，在 DB 中创建草稿，在 Redis 中缓存任务
     * 客户端凭 uploadId 逐片上传，支持续传判断
     */
    @GetMapping("/preUploadVideo")
    public ResponseVO<PreUploadVO> preUploadVideo(@RequestParam String fileName,
                                                  @RequestParam Integer totalChunks) {
        String userId = UserContext.get().getUserId();
        PreUploadVO vo = videoInfoService.preUploadVideo(fileName, totalChunks, userId);
        return ResponseVO.ok(vo);
    }

    /**
     * 单分片上传：将分片写入临时目录；所有分片到齐后触发异步转码
     * 路径安全：uploadId 只允许字母数字，防止路径穿越攻击
     */
    @PostMapping("/uploadVideo")
    public ResponseVO<Void> uploadVideo(@RequestParam String uploadId,
                                        @RequestParam Integer chunkIndex,
                                        @RequestParam MultipartFile file) throws IOException {
        // 路径安全校验：uploadId 只允许字母和数字
        if (!uploadId.matches("[a-zA-Z0-9]+")) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        // 校验上传任务是否存在且属于当前用户
        String taskJson = redisUtils.get(RedisUtils.UPLOAD_TASK_PREFIX + uploadId);
        if (taskJson == null) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }
        UploadTaskVO task = JSON.parseObject(taskJson, UploadTaskVO.class);
        String currentUserId = UserContext.get().getUserId();
        if (!currentUserId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }
        if (chunkIndex < 0 || chunkIndex >= task.getTotalChunks()) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        // 将分片写入临时目录
        File tempDir = new File(projectFolder + "temp/" + uploadId + "/");
        tempDir.mkdirs();
        file.transferTo(new File(tempDir, String.valueOf(chunkIndex)));

        // 更新已上传分片数
        task.setUploadedChunks(task.getUploadedChunks() + 1);
        redisUtils.set(RedisUtils.UPLOAD_TASK_PREFIX + uploadId, JSON.toJSONString(task), RedisUtils.UPLOAD_TASK_TTL);

        // 所有分片到齐后触发异步转码，不阻塞当前 HTTP 响应
        if (task.getUploadedChunks().equals(task.getTotalChunks())) {
            videoTranscodeService.mergeAndTranscode(uploadId, task.getVideoId(), task.getFileName(), task.getTotalChunks());
        }

        return ResponseVO.ok();
    }

    /**
     * 取消上传：销毁 Redis 任务记录，删除已上传的临时分片，释放磁盘空间
     */
    /**
     * 取消上传：同步清理 DB、Redis、temp 目录、封面图；
     * video 输出目录若转码正在进行则放入 Redis 队列，由定时任务异步删除
     * @param coverPath 封面相对路径（如 images/video/xxx.jpg），未上传封面时不传
     */
    @GetMapping("/delUploadVideo")
    public ResponseVO<Void> delUploadVideo(@RequestParam String uploadId,
                                           @RequestParam String videoId,
                                           @RequestParam(required = false) String coverPath) {
        // 删除 DB 记录（VideoInfo 草稿 + VideoInfoFile，若已入库）
        videoInfoService.removeById(videoId);
        videoInfoFileService.removeByVideoId(videoId);

        // 清理 Redis 上传任务（若转码已完成则任务已不存在，忽略）
        redisUtils.delete(RedisUtils.UPLOAD_TASK_PREFIX + uploadId);

        // 清理分片临时目录
        FileUtils.deleteQuietly(new File(projectFolder + "temp/" + uploadId + "/"));

        // 删除封面图：兼容传入完整 URL（含 path= 参数）和相对路径两种情况
        if (coverPath != null && !coverPath.isEmpty()) {
            String relativePath = coverPath.contains("path=")
                    ? coverPath.substring(coverPath.indexOf("path=") + 5)
                    : coverPath;
            if (!relativePath.contains("..") && !relativePath.contains("\\")) {
                FileUtils.deleteQuietly(new File(projectFolder + relativePath));
            }
        }

        // 尝试立即删除 HLS 输出目录；若转码仍在进行则文件被占用，加入队列等待定时清理
        File videoDir = new File(projectFolder + "video/" + videoId + "/");
        if (videoDir.exists()) {
            boolean deleted = FileUtils.deleteQuietly(videoDir);
            if (!deleted) {
                // 转码进行中无法删除，加入待清理队列
                redisUtils.sAdd(RedisUtils.VIDEO_DELETE_QUEUE, videoId);
            }
        }
        return ResponseVO.ok();
    }

    /**
     * 上传视频封面图，返回可直接用于 <img src> 的完整 URL
     */
    @PostMapping("/uploadImage")
    public ResponseVO<String> uploadImage(MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dir = new File(projectFolder + "images/video/");
        dir.mkdirs();
        file.transferTo(new File(dir, filename));

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort();
        return ResponseVO.ok(baseUrl + "/api/v1/client/file/getResource?path=images/video/" + filename);
    }

    /**
     * 读取静态图片资源并设置浏览器长缓存
     * 增加路径安全校验防止目录穿越攻击（拒绝 .. 等路径操控符）
     */
    @GetMapping("/getResource")
    public void getResource(@RequestParam String path, HttpServletResponse response) throws IOException {
        // 防止路径穿越：只允许访问 project.folder 内的文件
        if (path.contains("..") || path.contains("\\")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        File file = new File(projectFolder + path);
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 设置浏览器缓存 7 天，减少重复请求
        response.setHeader("Cache-Control", "max-age=" + IMAGE_CACHE_MAX_AGE);
        response.setContentType(Files.probeContentType(file.toPath()));
        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * 读取 m3u8 索引文件，提供 HLS 播放入口
     * 同时上报本次播放行为到 Redis（播放量计数）
     * 访问前校验：审核通过 + 未删除 + 公开状态，否则 403
     */
    @GetMapping("/videoResource/{fileId}")
    public void getM3u8(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        VideoInfoFile fileInfo = videoInfoFileService.getByFileId(fileId);
        if (fileInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        VideoInfo videoInfo = videoInfoService.getVideoById(fileInfo.getVideoId());
        // 前台播放可见规则：审核通过(2) + 未逻辑删除(0) + 公开(1)
        if (videoInfo == null
                || videoInfo.getAuditStatus() != 2
                || videoInfo.getIsDeleted() != 0
                || videoInfo.getIsPublic() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 上报播放行为至 Redis，不同步落库（后续定时任务批量写入 DB）
        redisUtils.incr(RedisUtils.VIDEO_PLAY_PREFIX + fileInfo.getVideoId());

        File m3u8File = new File(projectFolder + fileInfo.getFilePath() + "index.m3u8");
        if (!m3u8File.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 将 m3u8 中的相对 ts 路径（如 0.ts）改写为含 fileId 的相对路径（如 {fileId}/0.ts）
        // 否则 hls.js 会把 0.ts 拼到 m3u8 URL 后面，导致 ts 请求路径错误
        String m3u8Content = new String(Files.readAllBytes(m3u8File.toPath()));
        m3u8Content = m3u8Content.replaceAll("(\\d+\\.ts)", fileId + "/$1");

        response.setContentType("application/vnd.apple.mpegurl");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(m3u8Content);
    }

    /**
     * 读取 ts 视频分片文件，供播放器分段加载实现 HLS 流式播放
     */
    @GetMapping("/videoResource/{fileId}/{tsName}")
    public void getTsSegment(@PathVariable String fileId,
                             @PathVariable String tsName,
                             HttpServletResponse response) throws IOException {
        VideoInfoFile fileInfo = videoInfoFileService.getByFileId(fileId);
        if (fileInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // ts 文件名安全校验：只允许数字+.ts
        if (!tsName.matches("\\d+\\.ts")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File tsFile = new File(projectFolder + fileInfo.getFilePath() + tsName);
        if (!tsFile.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("video/mp2t");
        Files.copy(tsFile.toPath(), response.getOutputStream());
    }
}
