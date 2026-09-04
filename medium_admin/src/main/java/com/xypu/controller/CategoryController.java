package com.xypu.controller;

import com.xypu.entity.dto.CategoryDTO;
import com.xypu.entity.dto.CategorySortDTO;
import com.xypu.entity.po.CategoryInfo;
import com.xypu.response.ResponseVO;
import com.xypu.service.CategoryInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/category")
public class CategoryController {

    @Resource
    private CategoryInfoService categoryInfoService;

    /**
     * 新增或更新分类
     * categoryId 为空时执行新增，不为空时执行更新；操作后自动刷新 Redis 缓存
     */
    @PostMapping("/saveCategory")
    public ResponseVO<Void> saveCategory(@RequestBody @Validated CategoryDTO dto) {
        categoryInfoService.saveCategory(dto);
        return ResponseVO.ok();
    }

    /**
     * 删除分类
     * 若存在子分类则拒绝删除并返回错误码 606
     */
    @PostMapping("/delCategory")
    public ResponseVO<Void> delCategory(@RequestParam Integer categoryId) {
        categoryInfoService.delCategory(categoryId);
        return ResponseVO.ok();
    }

    /**
     * 获取全量分类树
     * Cache-Aside 策略：优先读 Redis，缓存未命中时从 DB 重建后写入缓存
     */
    @GetMapping("/loadCategory")
    public ResponseVO<List<CategoryInfo>> loadCategory() {
        return ResponseVO.ok(categoryInfoService.loadAllCategoryTree());
    }

    /**
     * 批量更新同级分类排序
     * 前端拖拽完成后，将调整后的完整顺序列表一次性提交，后端逐条写入 sort 字段
     */
    @PostMapping("/updateSort")
    public ResponseVO<Void> updateSort(@RequestBody @Validated List<CategorySortDTO> sortList) {
        categoryInfoService.updateSort(sortList);
        return ResponseVO.ok();
    }
}
