package com.xypu.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveVideoInfoDTO {

    /** 预上传时生成的视频ID，必须属于当前登录用户 */
    @NotEmpty(message = "视频ID不能为空")
    private String videoId;

    @NotEmpty(message = "视频名称不能为空")
    private String videoName;

    @NotEmpty(message = "视频封面不能为空")
    private String videoCover;

    @NotNull(message = "父级分类不能为空")
    private Integer pCategoryId;

    private Integer categoryId;

    private String tags;

    private String introduction;

    /** 互动配置：0=全部开启 1=关闭评论 2=关闭弹幕 */
    private Integer interaction;

    /** 是否公开：0=私密 1=公开 */
    private Integer isPublic;

    /** 是否VIP专属：0=免费 1=VIP */
    private Integer isVip;
}
