package com.xypu.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryDTO {

    /** 父级分类ID，0 表示顶级分类，不可为空 */
    @JsonProperty("pCategoryId")
    @NotNull(message = "父级分类ID不能为空")
    private Integer pCategoryId;

    /** 分类ID，为空时执行新增，不为空时执行更新 */
    private Integer categoryId;

    @NotEmpty(message = "分类编码不能为空")
    private String categoryCode;

    @NotEmpty(message = "分类名称不能为空")
    private String categoryName;

    private String icon;

    private String background;

    /** 排序号，数字越小展示越靠前 */
    private Integer sort;
}
