package com.xypu.entity.vo;

import lombok.Data;

import java.util.Date;

/**
 * 视频列表/详情视图对象，包含视频主表字段 + 关联的用户昵称和 HLS 播放凭证
 */
@Data
public class VideoInfoVO {

    private String videoId;
    private String videoCover;
    private String videoName;
    private String userId;
    private Date createTime;
    private Date lastUpdateTime;
    private Integer pCategoryId;
    private Integer categoryId;
    private String tags;
    private String introduction;
    private Integer interaction;
    private Long duration;
    private Long playCount;
    private Integer recommendType;
    private Date recommendTime;
    private Integer isPublic;
    private Integer isVip;
    private Integer auditStatus;
    private Integer isDeleted;

    /** 关联 user_info.nick_name */
    private String nickName;

    /** 关联 video_info_file.file_id，详情接口使用，列表接口为 null */
    private String fileId;
}
