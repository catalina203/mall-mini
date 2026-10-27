package com.mallmini.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductImage;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductImageMapper;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.vo.ProductDetailVO;
import com.mallmini.product.vo.ProductSummaryVO;
import com.mallmini.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SearchService searchService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productMapper,
                productSkuMapper,
                productImageMapper,
                categoryService,
                new SpecJsonParser(new ObjectMapper()),
                searchService);
    }

    @Test
    void aggregatesEnabledSkuPriceAndStock() {
        Product product = product(1L, "样例耳机", "ON_SALE");
        Page<Product> page = new Page<>(1, 12);
        page.setRecords(List.of(product));
        page.setTotal(1);
        when(productMapper.selectOnSalePage(any(), isNull(), isNull(), isNull(), isNull(), eq("new")))
                .thenReturn(page);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(
                sku(1L, 1L, "黑色", "199.00", 50, "ENABLED"),
                sku(2L, 1L, "白色", "219.00", 30, "ENABLED")));

        PageResult<ProductSummaryVO> result = productService.listOnSale(1, 12, null, null, "new", null, null);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        ProductSummaryVO summary = result.items().get(0);
        assertThat(summary.minPrice()).isEqualByComparingTo("199.00");
        assertThat(summary.maxPrice()).isEqualByComparingTo("219.00");
        assertThat(summary.totalStock()).isEqualTo(80);
    }

    @Test
    void rejectsInvalidPriceRange() {
        assertThatThrownBy(() -> productService.listOnSale(
                1, 12, null, null, "new", new BigDecimal("100"), new BigDecimal("10")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最低价");
    }

    @Test
    void rejectsOffSaleProductDetail() {
        Product product = product(13L, "已下架耳机", "OFF_SALE");
        when(productMapper.selectById(13L)).thenReturn(product);

        assertThatThrownBy(() -> productService.getDetail(13L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品不存在或已下架");
    }

    @Test
    void rejectsProductWithoutEnabledSku() {
        Product product = product(1L, "样例耳机", "ON_SALE");
        when(productMapper.selectById(1L)).thenReturn(product);
        when(categoryService.isEnabled(12L)).thenReturn(true);
        when(productImageMapper.selectList(any())).thenReturn(List.of());
        when(productSkuMapper.selectList(any())).thenReturn(List.of(
                sku(1L, 1L, "黑色", "199.00", 0, "DISABLED")));

        assertThatThrownBy(() -> productService.getDetail(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可购买");
    }

    @Test
    void returnsDetailWithSpecAndImages() {
        Product product = product(1L, "样例耳机", "ON_SALE");
        when(productMapper.selectById(1L)).thenReturn(product);
        when(categoryService.isEnabled(12L)).thenReturn(true);
        ProductImage image = new ProductImage();
        image.setId(10L);
        image.setProductId(1L);
        image.setImageUrl("http://localhost/demo.svg");
        image.setSortOrder(0);
        when(productImageMapper.selectList(any())).thenReturn(List.of(image));
        when(productSkuMapper.selectList(any())).thenReturn(List.of(
                sku(1L, 1L, "黑色", "199.00", 50, "ENABLED")));

        ProductDetailVO detail = productService.getDetail(1L);

        assertThat(detail.name()).isEqualTo("样例耳机");
        assertThat(detail.images()).hasSize(1);
        assertThat(detail.skus()).hasSize(1);
        assertThat(detail.skus().get(0).spec()).containsEntry("颜色", "黑色");
    }

    private Product product(Long id, String name, String status) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(12L);
        product.setName(name);
        product.setSubtitle("样例");
        product.setDescription("样例商品");
        product.setMainImage("http://localhost/demo.svg");
        product.setStatus(status);
        product.setSortOrder(0);
        return product;
    }

    private ProductSku sku(Long id, Long productId, String color, String price, int stock, String status) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setSkuCode("SKU-" + id);
        sku.setSpecJson("{\"颜色\":\"" + color + "\"}");
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setStatus(status);
        return sku;
    }
}
