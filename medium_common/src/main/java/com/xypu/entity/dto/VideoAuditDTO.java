package com.xypu.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class VideoAuditDTO {

    @NotEmpty(message = "视频ID不能为空")
    private String videoId;

    /** 审核结果：2=审核通过 3=驳回 */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    /** 驳回原因（auditStatus=3 时填写） */
    private String rejectReason;
}
