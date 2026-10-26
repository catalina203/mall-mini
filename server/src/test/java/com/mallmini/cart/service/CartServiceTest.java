package com.mallmini.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.cart.dto.AddCartItemRequest;
import com.mallmini.cart.dto.CartSelectionRequest;
import com.mallmini.cart.dto.UpdateCartItemRequest;
import com.mallmini.cart.entity.CartItem;
import com.mallmini.cart.mapper.CartItemMapper;
import com.mallmini.cart.vo.CartVO;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryService categoryService;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartItemMapper,
                productSkuMapper,
                productMapper,
                categoryService,
                new SpecJsonParser(new ObjectMapper()));
    }

    @Test
    void aggregatesSelectedAvailableItems() {
        when(cartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(1L, 11L, 2, true),
                cartItem(2L, 12L, 5, true)));
        when(productSkuMapper.selectBatchIds(any())).thenReturn(List.of(
                sku(11L, 1L, "10.00", 10, "ENABLED"),
                sku(12L, 1L, "20.00", 2, "ENABLED")));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(1L, "ON_SALE")));
        when(categoryService.isEnabled(5L)).thenReturn(true);

        CartVO cart = cartService.list(1L);

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.items().get(0).available()).isTrue();
        assertThat(cart.items().get(0).subtotal()).isEqualByComparingTo("20.00");
        assertThat(cart.items().get(1).available()).isFalse();
        assertThat(cart.items().get(1).unavailableReason()).contains("库存不足");
        assertThat(cart.summary().selectedItemCount()).isEqualTo(1);
        assertThat(cart.summary().selectedQuantity()).isEqualTo(2);
        assertThat(cart.summary().selectedAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsAddingOffSaleProduct() {
        when(productSkuMapper.selectById(11L)).thenReturn(sku(11L, 1L, "10.00", 10, "ENABLED"));
        when(productMapper.selectById(1L)).thenReturn(product(1L, "OFF_SALE"));

        assertThatThrownBy(() -> cartService.add(1L, new AddCartItemRequest(11L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可购买");
        verify(cartItemMapper, never()).insert(any(CartItem.class));
    }

    @Test
    void rejectsQuantityOverLimitWhenAdding() {
        when(productSkuMapper.selectById(11L)).thenReturn(sku(11L, 1L, "10.00", 10, "ENABLED"));
        when(productMapper.selectById(1L)).thenReturn(product(1L, "ON_SALE"));
        when(categoryService.isEnabled(5L)).thenReturn(true);
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(1L, 11L, 90, true));

        assertThatThrownBy(() -> cartService.add(1L, new AddCartItemRequest(11L, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateRejectsOtherUsersItem() {
        CartItem item = cartItem(1L, 11L, 1, true);
        item.setUserId(2L);
        when(cartItemMapper.selectById(1L)).thenReturn(item);

        assertThatThrownBy(() -> cartService.update(1L, 1L, new UpdateCartItemRequest(2, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车项不存在");
    }

    @Test
    void selectingEmptyIdListDoesNotTouchAllItems() {
        when(cartItemMapper.selectList(any())).thenReturn(List.of());

        CartVO cart = cartService.select(1L, new CartSelectionRequest(true, List.of()));

        assertThat(cart.items()).isEmpty();
        verify(cartItemMapper, never()).update(any(), any());
    }

    @Test
    void addNewItemInsertsAndReloads() {
        when(productSkuMapper.selectById(11L)).thenReturn(sku(11L, 1L, "10.00", 10, "ENABLED"));
        when(productMapper.selectById(1L)).thenReturn(product(1L, "ON_SALE"));
        when(categoryService.isEnabled(5L)).thenReturn(true);
        when(cartItemMapper.selectOne(any())).thenReturn(null);
        when(cartItemMapper.selectList(any())).thenReturn(List.of());

        cartService.add(1L, new AddCartItemRequest(11L, 2));

        verify(cartItemMapper).insert(any(CartItem.class));
    }

    private CartItem cartItem(Long id, Long skuId, int quantity, boolean selected) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(1L);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setSelected(selected);
        return item;
    }

    private ProductSku sku(Long id, Long productId, String price, int stock, String status) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setSkuCode("SKU-" + id);
        sku.setSpecJson("{\"颜色\":\"黑色\"}");
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setStatus(status);
        return sku;
    }

    private Product product(Long id, String status) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(5L);
        product.setName("样例商品");
        product.setMainImage("");
        product.setStatus(status);
        return product;
    }
}
