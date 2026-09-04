package com.xypu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("category_info")
public class CategoryInfo {

    @TableId(type = IdType.AUTO)
    private Integer categoryId;

    private String categoryCode;

    private String categoryName;

    private Integer pCategoryId;

    private String icon;

    private String background;

    private Integer sort;

    /** 非DB字段，树形结构的子分类列表，仅在内存/Redis中使用 */
    @TableField(exist = false)
    private List<CategoryInfo> children;
}
