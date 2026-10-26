package com.mallmini.admin.service;

import com.mallmini.category.dto.CategorySaveRequest;
import com.mallmini.category.entity.Category;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.category.service.AdminCategoryService;
import com.mallmini.category.vo.AdminCategoryVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.product.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private AdminOperationLogger operationLogger;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    @Test
    void createsSecondLevelCategory() {
        Category parent = category(1L, 0L, "数码");
        when(categoryMapper.selectById(1L)).thenReturn(parent);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(11L);
            return 1;
        });

        AdminCategoryVO result = adminCategoryService.create(
                new CategorySaveRequest("耳机", 1L, "", 1, "ENABLED"), 99L);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.parentId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("ENABLED");
        verify(operationLogger).log(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsThirdLevelCategory() {
        Category parent = category(11L, 1L, "耳机");
        when(categoryMapper.selectById(11L)).thenReturn(parent);

        assertThatThrownBy(() -> adminCategoryService.create(
                new CategorySaveRequest("降噪耳机", 11L, "", 1, "ENABLED"), 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("两级分类");
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void rejectsDeleteWhenProductsExist() {
        Category category = category(3L, 0L, "家居");
        when(categoryMapper.selectById(3L)).thenReturn(category);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> adminCategoryService.delete(3L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在商品");
        verify(categoryMapper, never()).deleteById(3L);
    }

    private Category category(Long id, Long parentId, String name) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setIconUrl("");
        category.setSortOrder(0);
        category.setStatus("ENABLED");
        return category;
    }
}
