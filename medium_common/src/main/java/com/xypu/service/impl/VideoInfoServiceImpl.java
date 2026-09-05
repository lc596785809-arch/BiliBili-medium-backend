package com.xypu.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xypu.entity.dto.SaveVideoInfoDTO;
import com.xypu.entity.dto.VideoAuditDTO;
import com.xypu.entity.dto.VideoQueryDTO;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.vo.PreUploadVO;
import com.xypu.entity.vo.UploadTaskVO;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.mapper.VideoInfoMapper;
import com.xypu.service.VideoInfoService;
import com.xypu.utils.RedisUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

@Service
public class VideoInfoServiceImpl extends ServiceImpl<VideoInfoMapper, VideoInfo> implements VideoInfoService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public PreUploadVO preUploadVideo(String fileName, Integer totalChunks, String userId) {
        // 生成上传唯一标识和视频ID
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String videoId = RandomStringUtils.randomAlphanumeric(10);

        // 在 DB 中预创建草稿记录，转码完成前 auditStatus 保持为 0
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setVideoId(videoId);
        videoInfo.setUserId(userId);
        // 占位默认值，saveVideoInfo 提交时会覆盖
        videoInfo.setVideoName(fileName);
        videoInfo.setVideoCover("");
        videoInfo.setPCategoryId(0);
        videoInfo.setAuditStatus(0);
        videoInfo.setIsPublic(1);
        videoInfo.setIsVip(0);
        videoInfo.setIsDeleted(0);
        videoInfo.setPlayCount(0L);
        videoInfo.setLikeCount(0L);
        videoInfo.setDanmuCount(0L);
        videoInfo.setCommentCount(0L);
        videoInfo.setCoinCount(0L);
        videoInfo.setCollectCount(0L);
        videoInfo.setRecommendType(0);
        videoInfo.setInteraction(0);
        Date now = new Date();
        videoInfo.setCreateTime(now);
        videoInfo.setLastUpdateTime(now);
        save(videoInfo);

        // 将上传任务写入 Redis，供后续分片上传校验使用
        UploadTaskVO task = new UploadTaskVO();
        task.setUserId(userId);
        task.setVideoId(videoId);
        task.setFileName(fileName);
        task.setTotalChunks(totalChunks);
        task.setUploadedChunks(0);
        redisUtils.set(RedisUtils.UPLOAD_TASK_PREFIX + uploadId, JSON.toJSONString(task), RedisUtils.UPLOAD_TASK_TTL);

        PreUploadVO vo = new PreUploadVO();
        vo.setUploadId(uploadId);
        vo.setVideoId(videoId);
        return vo;
    }

    @Override
    public void saveVideoInfo(SaveVideoInfoDTO dto, String userId) {
        VideoInfo existing = getById(dto.getVideoId());
        if (existing == null || !userId.equals(existing.getUserId())) {
            // 视频不存在或不属于当前用户，拒绝操作
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        VideoInfo update = new VideoInfo();
        BeanUtils.copyProperties(dto, update);
        // 提交信息后进入待审核状态
        update.setAuditStatus(1);
        update.setLastUpdateTime(new Date());
        updateById(update);
    }

    @Override
    public IPage<VideoInfo> loadVideoList(VideoQueryDTO dto) {
        Page<VideoInfo> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<VideoInfo> wrapper = new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getIsDeleted, 0)
                .like(StringUtils.isNotBlank(dto.getVideoName()), VideoInfo::getVideoName, dto.getVideoName())
                .eq(dto.getAuditStatus() != null, VideoInfo::getAuditStatus, dto.getAuditStatus())
                .eq(dto.getRecommendType() != null, VideoInfo::getRecommendType, dto.getRecommendType())
                .orderByDesc(VideoInfo::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public void auditVideo(VideoAuditDTO dto) {
        VideoInfo update = new VideoInfo();
        update.setVideoId(dto.getVideoId());
        update.setAuditStatus(dto.getAuditStatus());
        update.setLastUpdateTime(new Date());
        updateById(update);
    }

    @Override
    public void recommendVideo(String videoId, Integer recommendType) {
        // 使用 LambdaUpdateWrapper 显式控制每个字段，确保取消推荐时 recommend_time 能被清空为 NULL
        update(new LambdaUpdateWrapper<VideoInfo>()
                .eq(VideoInfo::getVideoId, videoId)
                .set(VideoInfo::getRecommendType, recommendType)
                .set(VideoInfo::getRecommendTime, recommendType == 1 ? new Date() : null)
                .set(VideoInfo::getLastUpdateTime, new Date()));
    }

    @Override
    public void deleteVideo(String videoId) {
        VideoInfo update = new VideoInfo();
        update.setVideoId(videoId);
        update.setIsDeleted(1);
        update.setLastUpdateTime(new Date());
        updateById(update);
    }

    @Override
    public long getVideoCount() {
        return count(new LambdaQueryWrapper<VideoInfo>().eq(VideoInfo::getIsDeleted, 0));
    }

    @Override
    public void setVideoPublic(String videoId, Integer isPublic) {
        VideoInfo update = new VideoInfo();
        update.setVideoId(videoId);
        update.setIsPublic(isPublic);
        update.setLastUpdateTime(new Date());
        updateById(update);
    }

    @Override
    public VideoInfo getVideoById(String videoId) {
        return getById(videoId);
    }
}
