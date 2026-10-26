package com.mallmini.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.category.entity.Category;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.product.dto.ProductSaveRequest;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductImageMapper;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.AdminProductService;
import com.mallmini.product.service.SpecJsonParser;
import com.mallmini.product.vo.AdminProductDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductImageMapper productImageMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private MallOrderItemMapper orderItemMapper;
    @Mock
    private AdminOperationLogger operationLogger;

    private AdminProductService adminProductService;

    @BeforeEach
    void setUp() {
        adminProductService = new AdminProductService(
                productMapper,
                productSkuMapper,
                productImageMapper,
                categoryMapper,
                orderItemMapper,
                new SpecJsonParser(new ObjectMapper()),
                new ObjectMapper(),
                operationLogger);
    }

    @Test
    void createsProductWithSku() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        when(productSkuMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(5L);
            return 1;
        });

        AdminProductDetailVO detail = adminProductService.create(request("ON_SALE"), 99L);

        assertThat(detail.id()).isEqualTo(5L);
        assertThat(detail.status()).isEqualTo("ON_SALE");
        verify(productSkuMapper).insert(any(ProductSku.class));
        verify(operationLogger).log(eq(99L), eq("PRODUCT_CREATE"), eq("PRODUCT"), eq("5"), any());
    }

    @Test
    void rejectsOnSaleWithoutEnabledSku() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        when(productSkuMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> adminProductService.create(request("ON_SALE", "DISABLED"), 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("启用的 SKU");
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void rejectsDeleteWhenReferencedByOrder() {
        Product product = new Product();
        product.setId(5L);
        product.setName("样例商品");
        when(productMapper.selectById(5L)).thenReturn(product);
        when(orderItemMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> adminProductService.delete(5L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单引用");
        verify(productMapper, never()).deleteById(5L);
    }

    private ProductSaveRequest request(String status) {
        return request(status, "ENABLED");
    }

    private ProductSaveRequest request(String status, String skuStatus) {
        return new ProductSaveRequest(
                "样例商品",
                "副标题",
                "描述",
                1L,
                "",
                0,
                status,
                List.of(),
                List.of(new ProductSaveRequest.SkuSaveItem(
                        null, "SKU-NEW", Map.of("颜色", "黑色"),
                        new BigDecimal("99.00"), 10, skuStatus)));
    }

    private Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setParentId(0L);
        category.setName("数码");
        category.setStatus("ENABLED");
        return category;
    }
}
