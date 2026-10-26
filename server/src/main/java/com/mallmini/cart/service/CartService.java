package com.mallmini.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mallmini.cart.dto.AddCartItemRequest;
import com.mallmini.cart.dto.CartSelectionRequest;
import com.mallmini.cart.dto.UpdateCartItemRequest;
import com.mallmini.cart.entity.CartItem;
import com.mallmini.cart.mapper.CartItemMapper;
import com.mallmini.cart.vo.CartItemVO;
import com.mallmini.cart.vo.CartSummaryVO;
import com.mallmini.cart.vo.CartVO;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.enums.ProductStatus;
import com.mallmini.product.enums.SkuStatus;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final SpecJsonParser specJsonParser;

    public CartVO list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return emptyCart();
        }

        Set<Long> skuIds = items.stream().map(CartItem::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuById = productSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));

        Set<Long> productIds = skuById.values().stream()
                .map(ProductSku::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> productById = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        Map<Long, Boolean> categoryEnabled = new HashMap<>();
        List<CartItemVO> itemVOs = new ArrayList<>();
        int selectedItemCount = 0;
        int selectedQuantity = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductSku sku = skuById.get(item.getSkuId());
            Product product = sku == null ? null : productById.get(sku.getProductId());
            int quantity = Math.max(item.getQuantity() == null ? 0 : item.getQuantity(), 0);
            BigDecimal price = sku == null || sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            int stock = sku == null || sku.getStock() == null ? 0 : Math.max(sku.getStock(), 0);
            boolean selected = Boolean.TRUE.equals(item.getSelected());

            String unavailableReason = "";
            if (sku == null) {
                unavailableReason = "规格已失效";
            } else if (!SkuStatus.ENABLED.name().equals(sku.getStatus())) {
                unavailableReason = "规格已停用";
            } else if (product == null) {
                unavailableReason = "商品已失效";
            } else if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
                unavailableReason = "商品已下架";
            } else if (!categoryEnabled.computeIfAbsent(product.getCategoryId(), categoryService::isEnabled)) {
                unavailableReason = "商品分类已停用";
            } else if (stock <= 0) {
                unavailableReason = "库存不足";
            } else if (stock < quantity) {
                unavailableReason = "库存不足（剩余 " + stock + " 件）";
            }

            boolean available = unavailableReason.isEmpty();
            CartItemVO vo = new CartItemVO(
                    item.getId(),
                    item.getSkuId(),
                    product == null ? null : product.getId(),
                    product == null ? "商品已失效" : product.getName(),
                    product == null ? "" : product.getMainImage(),
                    sku == null ? Map.of() : specJsonParser.parse(sku.getSpecJson()),
                    price,
                    stock,
                    quantity,
                    selected,
                    available,
                    unavailableReason,
                    subtotal);
            itemVOs.add(vo);

            if (selected && available) {
                selectedItemCount++;
                selectedQuantity += quantity;
                selectedAmount = selectedAmount.add(subtotal);
            }
        }

        return new CartVO(itemVOs, new CartSummaryVO(
                selectedItemCount,
                selectedQuantity,
                selectedAmount.setScale(2, RoundingMode.HALF_UP)));
    }

    public CartVO add(Long userId, AddCartItemRequest request) {
        ProductSku sku = requirePurchasableSku(request.skuId());

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, sku.getId()));
        if (existing != null) {
            int newQuantity = (existing.getQuantity() == null ? 0 : existing.getQuantity()) + request.quantity();
            if (newQuantity > MAX_QUANTITY) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "单项数量不能超过 99");
            }
            existing.setQuantity(newQuantity);
            existing.setSelected(true);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setSkuId(sku.getId());
            item.setQuantity(request.quantity());
            item.setSelected(true);
            cartItemMapper.insert(item);
        }
        return list(userId);
    }

    public CartVO update(Long userId, Long itemId, UpdateCartItemRequest request) {
        if (request.quantity() == null && request.selected() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未提供需要修改的内容");
        }
        CartItem item = requireOwned(userId, itemId);
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.selected() != null) {
            item.setSelected(request.selected());
        }
        cartItemMapper.updateById(item);
        return list(userId);
    }

    public CartVO delete(Long userId, Long itemId) {
        requireOwned(userId, itemId);
        cartItemMapper.deleteById(itemId);
        return list(userId);
    }

    public CartVO clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return emptyCart();
    }

    public CartVO select(Long userId, CartSelectionRequest request) {
        List<Long> ids = request.ids();
        if (ids != null && ids.isEmpty()) {
            return list(userId);
        }
        LambdaUpdateWrapper<CartItem> wrapper = new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .set(CartItem::getSelected, request.selected());
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(CartItem::getId, ids);
        }
        cartItemMapper.update(null, wrapper);
        return list(userId);
    }

    private ProductSku requirePurchasableSku(Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !SkuStatus.ENABLED.name().equals(sku.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "规格不存在或已停用");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null
                || !ProductStatus.ON_SALE.name().equals(product.getStatus())
                || !categoryService.isEnabled(product.getCategoryId())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品已下架或不可购买");
        }
        return sku;
    }

    private CartItem requireOwned(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车项不存在");
        }
        return item;
    }

    private CartVO emptyCart() {
        return new CartVO(List.of(), new CartSummaryVO(0, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
    }
}
