package com.xypu.entity.vo;

import lombok.Data;

/** 视频预上传响应，前端凭 uploadId 分片上传，凭 videoId 提交视频信息 */
@Data
public class PreUploadVO {
    private String uploadId;
    private String videoId;
}
