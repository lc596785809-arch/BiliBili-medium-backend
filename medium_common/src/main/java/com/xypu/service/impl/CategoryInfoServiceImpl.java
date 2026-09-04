package com.xypu.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xypu.entity.dto.CategoryDTO;
import com.xypu.entity.dto.CategorySortDTO;
import com.xypu.entity.po.CategoryInfo;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.mapper.CategoryInfoMapper;
import com.xypu.service.CategoryInfoService;
import com.xypu.utils.RedisUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CategoryInfoServiceImpl extends ServiceImpl<CategoryInfoMapper, CategoryInfo> implements CategoryInfoService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public void saveCategory(CategoryDTO dto) {
        CategoryInfo entity = new CategoryInfo();
        BeanUtils.copyProperties(dto, entity);

        if (dto.getCategoryId() == null) {
            // 新增时若前端未传 sort，自动计算同级最大 sort + 1，保证新分类排在末尾
            if (dto.getSort() == null) {
                CategoryInfo maxSortSibling = getOne(new LambdaQueryWrapper<CategoryInfo>()
                        .eq(CategoryInfo::getPCategoryId, dto.getPCategoryId())
                        .orderByDesc(CategoryInfo::getSort)
                        .last("LIMIT 1"));
                int nextSort = (maxSortSibling == null || maxSortSibling.getSort() == null)
                        ? 1 : maxSortSibling.getSort() + 1;
                entity.setSort(nextSort);
            }
            save(entity);
        } else {
            updateById(entity);
        }

        // 写操作后清空两个分类缓存，保证下次查询返回最新数据
        redisUtils.delete(RedisUtils.REDIS_KEY_CATEGORY_LIST);
        redisUtils.delete(RedisUtils.REDIS_KEY_ROOT_CATEGORY_LIST);
    }

    @Override
    public void delCategory(Integer categoryId) {
        // 检查是否存在子分类，防止产生孤儿节点
        long childCount = count(new LambdaQueryWrapper<CategoryInfo>()
                .eq(CategoryInfo::getPCategoryId, categoryId));
        if (childCount > 0) {
            throw new BusinessException(ErrorCodeEnum.CODE_606);
        }

        removeById(categoryId);

        // 删除后同步清空缓存
        redisUtils.delete(RedisUtils.REDIS_KEY_CATEGORY_LIST);
        redisUtils.delete(RedisUtils.REDIS_KEY_ROOT_CATEGORY_LIST);
    }

    @Override
    public List<CategoryInfo> loadAllCategoryTree() {
        // 优先读 Redis 缓存
        String cached = redisUtils.get(RedisUtils.REDIS_KEY_CATEGORY_LIST);
        if (cached != null) {
            return JSON.parseObject(cached, new TypeReference<List<CategoryInfo>>() {});
        }

        // 缓存未命中，从 DB 查全表并按 sort 升序排列，再递归构建树
        List<CategoryInfo> allList = list(new LambdaQueryWrapper<CategoryInfo>()
                .orderByAsc(CategoryInfo::getSort));
        List<CategoryInfo> tree = buildTree(0, allList);

        redisUtils.set(RedisUtils.REDIS_KEY_CATEGORY_LIST, JSON.toJSONString(tree), RedisUtils.CATEGORY_CACHE_TTL);
        return tree;
    }

    @Override
    public List<CategoryInfo> loadRootCategories() {
        // 优先读 Redis 缓存
        String cached = redisUtils.get(RedisUtils.REDIS_KEY_ROOT_CATEGORY_LIST);
        if (cached != null) {
            return JSON.parseObject(cached, new TypeReference<List<CategoryInfo>>() {});
        }

        List<CategoryInfo> roots = list(new LambdaQueryWrapper<CategoryInfo>()
                .eq(CategoryInfo::getPCategoryId, 0)
                .orderByAsc(CategoryInfo::getSort));

        redisUtils.set(RedisUtils.REDIS_KEY_ROOT_CATEGORY_LIST, JSON.toJSONString(roots), RedisUtils.CATEGORY_CACHE_TTL);
        return roots;
    }

    @Override
    public List<CategoryInfo> loadLastLevelCategories() {
        // 复用全量树缓存，避免重复查 DB
        List<CategoryInfo> tree = loadAllCategoryTree();
        List<CategoryInfo> leaves = new ArrayList<>();
        collectLeaves(tree, leaves);
        return leaves;
    }

    @Override
    public List<CategoryInfo> buildTree(Integer pCategoryId, List<CategoryInfo> allList) {
        List<CategoryInfo> result = new ArrayList<>();
        for (CategoryInfo item : allList) {
            if (pCategoryId.equals(item.getPCategoryId())) {
                item.setChildren(buildTree(item.getCategoryId(), allList));
                result.add(item);
            }
        }
        result.sort(Comparator.comparingInt(c -> (c.getSort() == null ? Integer.MAX_VALUE : c.getSort())));
        return result;
    }

    @Override
    public void updateSort(List<CategorySortDTO> sortList) {
        for (CategorySortDTO item : sortList) {
            CategoryInfo entity = new CategoryInfo();
            entity.setCategoryId(item.getCategoryId());
            entity.setSort(item.getSort());
            updateById(entity);
        }
        // 排序变更后清空缓存，前端下次查询即可感知新顺序
        redisUtils.delete(RedisUtils.REDIS_KEY_CATEGORY_LIST);
        redisUtils.delete(RedisUtils.REDIS_KEY_ROOT_CATEGORY_LIST);
    }

    /** 递归遍历树，将无子节点的叶子分类收集到 leaves 列表中 */
    private void collectLeaves(List<CategoryInfo> nodes, List<CategoryInfo> leaves) {
        if (nodes == null) {
            return;
        }
        for (CategoryInfo node : nodes) {
            if (node.getChildren() == null || node.getChildren().isEmpty()) {
                leaves.add(node);
            } else {
                collectLeaves(node.getChildren(), leaves);
            }
        }
    }
}
