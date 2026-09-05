package com.xypu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 视频文件信息表
 * 单视频不支持分P，每个 video_id 对应唯一一条记录（fileIndex=0）
 * fileId 作为 HLS 资源访问入口（/videoResource/{fileId}）
 */
@Data
@TableName("video_info_file")
public class VideoInfoFile {

    @TableId(type = IdType.INPUT)
    private String fileId;

    private String userId;

    private String videoId;

    private String fileName;

    /** 固定为 0（不支持多P） */
    private Integer fileIndex;

    private Long fileSize;

    /** HLS 资源目录相对路径，格式 video/{videoId}/ */
    private String filePath;

    private Integer duration;
}
