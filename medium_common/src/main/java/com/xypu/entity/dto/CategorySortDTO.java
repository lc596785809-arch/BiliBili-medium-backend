package com.xypu.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategorySortDTO {

    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;

    @NotNull(message = "排序号不能为空")
    private Integer sort;
}
