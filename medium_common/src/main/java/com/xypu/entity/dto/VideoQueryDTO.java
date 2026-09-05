package com.xypu.entity.dto;

import lombok.Data;

@Data
public class VideoQueryDTO {

    /** 视频名称，模糊查询 */
    private String videoName;

    /** 审核状态筛选：0=草稿 1=待审核 2=审核通过 3=驳回 */
    private Integer auditStatus;

    /** 推荐状态筛选：0=不推荐 1=推荐 */
    private Integer recommendType;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
