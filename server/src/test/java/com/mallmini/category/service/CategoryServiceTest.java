package com.mallmini.category.service;

import com.mallmini.category.entity.Category;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.category.vo.CategoryVO;
import com.mallmini.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void buildsEnabledCategoryTree() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "数码", "ENABLED", 1),
                category(2L, 0L, "服饰", "ENABLED", 2),
                category(11L, 1L, "手机", "ENABLED", 1)));

        List<CategoryVO> tree = categoryService.listEnabledTree();

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getName()).isEqualTo("数码");
        assertThat(tree.get(0).getChildren()).extracting(CategoryVO::getName).containsExactly("手机");
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    void dropsChildrenWhoseParentIsDisabled() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(11L, 1L, "手机", "ENABLED", 1)));

        List<CategoryVO> tree = categoryService.listEnabledTree();

        assertThat(tree).isEmpty();
    }

    @Test
    void collectsDescendantCategoryIds() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "数码", "ENABLED", 1),
                category(11L, 1L, "手机", "ENABLED", 1),
                category(111L, 11L, "智能手机", "ENABLED", 1),
                category(2L, 0L, "服饰", "ENABLED", 2)));

        List<Long> ids = categoryService.enabledCategoryIds(1L);

        assertThat(ids).containsExactly(1L, 11L, 111L);
    }

    @Test
    void rejectsUnknownCategory() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "数码", "ENABLED", 1)));

        assertThatThrownBy(() -> categoryService.enabledCategoryIds(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    void isEnabledChecksStatus() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "数码", "ENABLED", 1));
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, 0L, "服饰", "DISABLED", 2));

        assertThat(categoryService.isEnabled(1L)).isTrue();
        assertThat(categoryService.isEnabled(2L)).isFalse();
        assertThat(categoryService.isEnabled(null)).isFalse();
    }

    private Category category(Long id, Long parentId, String name, String status, int sortOrder) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setStatus(status);
        category.setSortOrder(sortOrder);
        category.setIconUrl("");
        return category;
    }
}
