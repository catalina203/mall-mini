package com.mallmini.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.category.dto.CategorySaveRequest;
import com.mallmini.category.entity.Category;
import com.mallmini.category.enums.CategoryStatus;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.category.vo.AdminCategoryVO;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.product.entity.Product;
import com.mallmini.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final AdminOperationLogger operationLogger;

    public PageResult<AdminCategoryVO> page(long page, long pageSize, String status, String keyword) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Category::getStatus, parseStatus(status).name());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Category::getName, keyword.trim());
        }

        Page<Category> categoryPage = categoryMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        Map<Long, Category> all = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<AdminCategoryVO> items = categoryPage.getRecords().stream()
                .map(category -> toVO(category, all))
                .toList();
        return PageResult.of(items, safePage, safePageSize, categoryPage.getTotal());
    }

    public List<AdminCategoryVO> options() {
        Map<Long, Category> all = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return all.values().stream()
                .sorted((left, right) -> {
                    int parentCompare = Long.compare(left.getParentId(), right.getParentId());
                    if (parentCompare != 0) {
                        return parentCompare;
                    }
                    return Integer.compare(left.getSortOrder(), right.getSortOrder());
                })
                .map(category -> toVO(category, all))
                .toList();
    }

    @Transactional
    public AdminCategoryVO create(CategorySaveRequest request, Long adminId) {
        long parentId = normalizeParentId(request.parentId());
        validateParent(parentId, null);
        validateName(parentId, request.name().trim(), null);

        Category category = new Category();
        category.setParentId(parentId);
        category.setName(request.name().trim());
        category.setIconUrl(request.iconUrl() == null ? "" : request.iconUrl().trim());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(parseStatus(request.status() == null ? CategoryStatus.ENABLED.name() : request.status()).name());
        categoryMapper.insert(category);
        operationLogger.log(adminId, "CATEGORY_CREATE", "CATEGORY", String.valueOf(category.getId()), category.getName());
        return toVO(category);
    }

    @Transactional
    public AdminCategoryVO update(Long id, CategorySaveRequest request, Long adminId) {
        Category category = require(id);
        long parentId = normalizeParentId(request.parentId());
        validateParent(parentId, id);
        if (parentId != 0 && categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在子分类，不能调整为二级分类");
        }
        validateName(parentId, request.name().trim(), id);

        category.setParentId(parentId);
        category.setName(request.name().trim());
        category.setIconUrl(request.iconUrl() == null ? "" : request.iconUrl().trim());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(parseStatus(request.status() == null ? category.getStatus() : request.status()).name());
        categoryMapper.updateById(category);
        operationLogger.log(adminId, "CATEGORY_UPDATE", "CATEGORY", String.valueOf(id), category.getName());
        return toVO(category);
    }

    @Transactional
    public void updateStatus(Long id, String status, Long adminId) {
        Category category = require(id);
        category.setStatus(parseStatus(status).name());
        categoryMapper.updateById(category);
        operationLogger.log(adminId, "CATEGORY_STATUS", "CATEGORY", String.valueOf(id), category.getStatus());
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        Category category = require(id);
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先删除子分类");
        }
        if (productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下存在商品，不能删除");
        }
        categoryMapper.deleteById(id);
        operationLogger.log(adminId, "CATEGORY_DELETE", "CATEGORY", String.valueOf(id), category.getName());
    }

    private void validateParent(long parentId, Long selfId) {
        if (parentId == 0) {
            return;
        }
        if (selfId != null && parentId == selfId) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不能将自身设为父分类");
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "父分类不存在");
        }
        if (parent.getParentId() != null && parent.getParentId() != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "首版仅支持两级分类");
        }
    }

    private void validateName(long parentId, String name, Long selfId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, name);
        if (selfId != null) {
            wrapper.ne(Category::getId, selfId);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "同级分类名称已存在");
        }
    }

    private Category require(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private CategoryStatus parseStatus(String status) {
        try {
            return CategoryStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分类状态不合法");
        }
    }

    private AdminCategoryVO toVO(Category category, Map<Long, Category> all) {
        Category parent = category.getParentId() == null ? null : all.get(category.getParentId());
        return new AdminCategoryVO(
                category.getId(),
                category.getParentId(),
                parent == null ? "" : parent.getName(),
                category.getName(),
                category.getIconUrl(),
                category.getSortOrder(),
                category.getStatus());
    }

    private AdminCategoryVO toVO(Category category) {
        Category parent = category.getParentId() == null || category.getParentId() == 0
                ? null : categoryMapper.selectById(category.getParentId());
        return new AdminCategoryVO(
                category.getId(),
                category.getParentId(),
                parent == null ? "" : parent.getName(),
                category.getName(),
                category.getIconUrl(),
                category.getSortOrder(),
                category.getStatus());
    }
}
