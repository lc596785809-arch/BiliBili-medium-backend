package com.xypu.controller;

import com.xypu.entity.dto.CategoryDTO;
import com.xypu.entity.dto.CategorySortDTO;
import com.xypu.entity.po.CategoryInfo;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import com.xypu.service.CategoryInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/category")
public class CategoryController {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Value("${project.folder}")
    private String projectFolder;

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
     * 上传分类图标或背景图
     * 文件保存到 project.folder/images/category/ 目录，返回可直接用于 <img src> 的完整 URL
     */
    @PostMapping("/uploadImage")
    public ResponseVO<String> uploadImage(MultipartFile file, HttpServletRequest request) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        // 校验文件扩展名，拒绝非图片类型
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCodeEnum.CODE_600);
        }

        // 保存文件，使用 UUID 避免文件名冲突
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dir = new File(projectFolder + "images/category/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        file.transferTo(new File(dir, filename));

        // 拼接完整 URL：scheme + host + port + contextPath + 静态资源路径
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath();
        return ResponseVO.ok(baseUrl + "/images/category/" + filename);
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
