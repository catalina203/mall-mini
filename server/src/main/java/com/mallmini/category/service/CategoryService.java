package com.mallmini.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.category.entity.Category;
import com.mallmini.category.enums.CategoryStatus;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.category.vo.CategoryVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<CategoryVO> listEnabledTree() {
        List<Category> categories = listEnabled();
        Map<Long, CategoryVO> nodes = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setIconUrl(category.getIconUrl());
            vo.setSortOrder(category.getSortOrder());
            nodes.put(category.getId(), vo);
        }

        List<CategoryVO> roots = new ArrayList<>();
        for (Category category : categories) {
            CategoryVO node = nodes.get(category.getId());
            if (category.getParentId() == null || category.getParentId() == 0L) {
                roots.add(node);
                continue;
            }
            CategoryVO parent = nodes.get(category.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    public List<Long> enabledCategoryIds(Long categoryId) {
        List<Category> categories = listEnabled();
        Category target = categories.stream()
                .filter(category -> category.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在或已停用"));

        Map<Long, List<Long>> childrenByParent = new LinkedHashMap<>();
        for (Category category : categories) {
            childrenByParent.computeIfAbsent(category.getParentId(), key -> new ArrayList<>())
                    .add(category.getId());
        }

        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(target.getId());
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            result.add(current);
            queue.addAll(childrenByParent.getOrDefault(current, List.of()));
        }
        return result;
    }

    public boolean isEnabled(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category != null && CategoryStatus.ENABLED.name().equals(category.getStatus());
    }

    private List<Category> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, CategoryStatus.ENABLED.name())
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
    }
}
