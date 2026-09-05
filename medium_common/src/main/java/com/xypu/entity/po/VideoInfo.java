package com.xypu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 视频信息主表
 * auditStatus: 0=草稿(转码中/待填信息) 1=待审核 2=审核通过 3=驳回
 * isPublic: 0=私密 1=公开；isDeleted: 0=正常 1=逻辑删除
 */
@Data
@TableName("video_info")
public class VideoInfo {

    @TableId(type = IdType.INPUT)
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

    /** 互动配置：0=全部开启 1=关闭评论 2=关闭弹幕 */
    private Integer interaction;

    private Long duration;

    private Long playCount;

    private Long likeCount;

    private Long danmuCount;

    private Long commentCount;

    private Long coinCount;

    private Long collectCount;

    /** 是否推荐：0=不推荐 1=推荐 */
    private Integer recommendType;

    private Date recommendTime;

    private Date lastPlayTime;

    private Integer isPublic;

    private Integer isVip;

    private Integer auditStatus;

    private Integer isDeleted;
}
