package com.xypu.entity.vo;

import lombok.Data;

/** 存储在 Redis 中的上传任务信息，序列化为 JSON */
@Data
public class UploadTaskVO {

    private String userId;

    private String videoId;

    private String fileName;

    private Integer totalChunks;

    private Integer uploadedChunks;
}
