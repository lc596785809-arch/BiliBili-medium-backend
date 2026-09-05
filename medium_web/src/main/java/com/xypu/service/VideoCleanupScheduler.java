package com.xypu.service;

import com.xypu.utils.RedisUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Set;

/**
 * 视频目录定时清理任务
 * 处理转码进行中取消上传导致无法立即删除的 HLS 输出目录
 */
@Component
public class VideoCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoCleanupScheduler.class);

    @Value("${project.folder}")
    private String projectFolder;

    @Resource
    private RedisUtils redisUtils;

    /** 每 60 秒扫描一次待删除队列，逐个尝试删除 HLS 输出目录 */
    @Scheduled(fixedDelay = 60000)
    public void cleanPendingVideoDirs() {
        Set<String> pendingIds = redisUtils.sMembers(RedisUtils.VIDEO_DELETE_QUEUE);
        if (pendingIds == null || pendingIds.isEmpty()) {
            return;
        }
        for (String videoId : pendingIds) {
            File videoDir = new File(projectFolder + "video/" + videoId + "/");
            if (!videoDir.exists()) {
                // 目录已不存在（可能被转码后的检查逻辑清理），直接移出队列
                redisUtils.sRem(RedisUtils.VIDEO_DELETE_QUEUE, videoId);
                continue;
            }
            boolean deleted = FileUtils.deleteQuietly(videoDir);
            if (deleted) {
                redisUtils.sRem(RedisUtils.VIDEO_DELETE_QUEUE, videoId);
                log.info("已清理待删除视频目录，videoId={}", videoId);
            } else {
                // 仍被占用，下次重试
                log.warn("视频目录仍被占用，稍后重试，videoId={}", videoId);
            }
        }
    }
}
