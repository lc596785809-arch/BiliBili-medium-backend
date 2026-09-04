package com.xypu.controller;

import com.xypu.entity.po.CategoryInfo;
import com.xypu.response.ResponseVO;
import com.xypu.service.CategoryInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/client/category")
public class CategoryController {

    @Resource
    private CategoryInfoService categoryInfoService;

    /**
     * 获取全量分类树
     * Cache-Aside 策略：优先读 Redis，缓存未命中时从 DB 重建后写入缓存
     */
    @GetMapping("/loadCategory")
    public ResponseVO<List<CategoryInfo>> loadCategory() {
        return ResponseVO.ok(categoryInfoService.loadAllCategoryTree());
    }

    /**
     * 获取顶级分类列表（p_category_id = 0）
     * 用于首页导航栏等无需展示子分类的轻量场景
     */
    @GetMapping("/loadRootCategory")
    public ResponseVO<List<CategoryInfo>> loadRootCategory() {
        return ResponseVO.ok(categoryInfoService.loadRootCategories());
    }

    /**
     * 获取所有叶子分类（无子分类的末级节点）
     * 用于视频发布时强制选择最终分类，避免挂载到中间层级
     */
    @GetMapping("/loadLastLevelCategory")
    public ResponseVO<List<CategoryInfo>> loadLastLevelCategory() {
        return ResponseVO.ok(categoryInfoService.loadLastLevelCategories());
    }
}
