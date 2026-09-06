package com.xypu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xypu.entity.dto.SaveVideoInfoDTO;
import com.xypu.entity.dto.VideoAuditDTO;
import com.xypu.entity.dto.VideoQueryDTO;
import com.xypu.entity.po.VideoInfo;
import com.xypu.entity.vo.PreUploadVO;
import com.xypu.entity.vo.VideoInfoVO;

public interface VideoInfoService extends IService<VideoInfo> {

    /**
     * 预上传：在 DB 中创建草稿记录，并将上传任务信息写入 Redis
     * 返回 uploadId（分片上传凭证）和 videoId（提交视频信息时使用）
     */
    PreUploadVO preUploadVideo(String fileName, Integer totalChunks, String userId);

    /**
     * 提交视频信息（标题、封面、分类、标签、简介等）
     * 校验 videoId 归属当前用户；提交后 auditStatus 变为 1（待审核）
     */
    void saveVideoInfo(SaveVideoInfoDTO dto, String userId);

    /**
     * 分页查询视频列表（管理端）
     * 支持名称模糊、末级分类、审核状态、推荐状态、VIP 类型组合筛选
     * 关联 user_info 返回上传者昵称，关联 video_info_file 返回 fileId
     */
    IPage<VideoInfoVO> loadVideoList(VideoQueryDTO dto);

    /**
     * 查询视频详情（管理端），含昵称和 fileId
     */
    VideoInfoVO getVideoDetail(String videoId);

    /** 执行审核操作（通过=2 / 驳回=3） */
    void auditVideo(VideoAuditDTO dto);

    /** 设置首页推荐标识与推荐时间 */
    void recommendVideo(String videoId, Integer recommendType);

    /** 逻辑删除（isDeleted=1），不影响数据库原始记录 */
    void deleteVideo(String videoId);

    /** 统计未删除视频总数，用于后台数据看板 */
    long getVideoCount();

    /** 切换视频公开/私密状态（isPublic 0↔1） */
    void setVideoPublic(String videoId, Integer isPublic);

    /**
     * 切换 VIP 状态（isVip 0↔1）
     * 仅审核通过（auditStatus=2）的视频允许操作
     */
    void setVideoVip(String videoId, Integer isVip);

    /** 根据 videoId 获取视频信息 */
    VideoInfo getVideoById(String videoId);
}
