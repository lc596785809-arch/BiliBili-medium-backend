package com.xypu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xypu.entity.dto.CategoryDTO;
import com.xypu.entity.dto.CategorySortDTO;
import com.xypu.entity.po.CategoryInfo;

import java.util.List;

public interface CategoryInfoService extends IService<CategoryInfo> {

    /** 新增或更新分类，操作成功后清空 Redis 分类缓存 */
    void saveCategory(CategoryDTO dto);

    /**
     * 删除分类
     * 若该分类下存在子分类则拒绝删除，防止产生孤儿节点
     */
    void delCategory(Integer categoryId);

    /** 获取全量分类树（Cache-Aside：优先读 Redis，缓存未命中时从 DB 重建） */
    List<CategoryInfo> loadAllCategoryTree();

    /** 获取顶级分类列表（p_category_id = 0），用于首页导航等轻量展示场景 */
    List<CategoryInfo> loadRootCategories();

    /** 提取分类树中所有叶子节点（无子分类），用于视频发布时的终端分类选择 */
    List<CategoryInfo> loadLastLevelCategories();

    /** 递归构建以 pCategoryId 为根的子树，结果按 sort 字段升序排列 */
    List<CategoryInfo> buildTree(Integer pCategoryId, List<CategoryInfo> allList);

    /**
     * 批量更新同级分类的排序
     * 接收前端拖拽后的全量顺序列表，逐条更新 sort 字段并清空缓存
     */
    void updateSort(List<CategorySortDTO> sortList);
}
