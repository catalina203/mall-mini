package com.mallmini.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.category.entity.Category;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.inventory.enums.StockChangeType;
import com.mallmini.inventory.service.StockService;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.product.dto.ProductSaveRequest;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductImage;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.enums.ProductStatus;
import com.mallmini.product.enums.SkuStatus;
import com.mallmini.product.mapper.ProductImageMapper;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.vo.AdminProductDetailVO;
import com.mallmini.product.vo.AdminProductSummaryVO;
import com.mallmini.product.vo.ProductImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final SpecJsonParser specJsonParser;
    private final ObjectMapper objectMapper;
    private final AdminOperationLogger operationLogger;
    private final StockService stockService;

    public PageResult<AdminProductSummaryVO> page(long page,
                                                  long pageSize,
                                                  String status,
                                                  Long categoryId,
                                                  String keyword) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getSortOrder)
                .orderByDesc(Product::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Product::getStatus, parseProductStatus(status).name());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(condition -> condition
                    .like(Product::getName, normalized)
                    .or()
                    .like(Product::getSubtitle, normalized));
        }

        Page<Product> productPage = productMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        List<Product> products = productPage.getRecords();
        if (products.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, productPage.getTotal());
        }

        Map<Long, Category> categories = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, List<ProductSku>> skusByProduct = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .in(ProductSku::getProductId, products.stream().map(Product::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(ProductSku::getProductId));

        List<AdminProductSummaryVO> items = products.stream().map(product -> {
            List<ProductSku> skus = skusByProduct.getOrDefault(product.getId(), List.of());
            BigDecimal min = skus.stream().map(ProductSku::getPrice).filter(price -> price != null)
                    .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal max = skus.stream().map(ProductSku::getPrice).filter(price -> price != null)
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            int stock = skus.stream().mapToInt(sku -> sku.getStock() == null ? 0 : sku.getStock()).sum();
            Category category = categories.get(product.getCategoryId());
            return new AdminProductSummaryVO(
                    product.getId(),
                    product.getCategoryId(),
                    category == null ? "" : category.getName(),
                    product.getName(),
                    product.getSubtitle(),
                    product.getMainImage(),
                    product.getStatus(),
                    product.getSortOrder(),
                    min.setScale(2, RoundingMode.HALF_UP),
                    max.setScale(2, RoundingMode.HALF_UP),
                    stock,
                    skus.size(),
                    product.getUpdatedAt());
        }).toList();

        return PageResult.of(items, safePage, safePageSize, productPage.getTotal());
    }

    public AdminProductDetailVO detail(Long id) {
        Product product = require(id);
        return toDetail(product);
    }

    @Transactional
    public AdminProductDetailVO create(ProductSaveRequest request, Long adminId) {
        Category category = requireCategory(request.categoryId());
        ProductStatus status = parseProductStatus(request.status() == null
                ? ProductStatus.OFF_SALE.name() : request.status());
        validateSkuCodes(request.skus(), null);
        if (status == ProductStatus.ON_SALE && !hasEnabledSku(request.skus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上架商品至少需要一个启用的 SKU");
        }

        Product product = new Product();
        applyProduct(product, request, category.getId(), status);
        productMapper.insert(product);

        replaceImages(product.getId(), request.images());
        for (ProductSaveRequest.SkuSaveItem item : request.skus()) {
            ProductSku sku = insertSku(product.getId(), item);
            stockService.recordProductChange(sku, 0, sku.getStock() == null ? 0 : sku.getStock(),
                    StockChangeType.PRODUCT_INIT, adminId, "创建商品初始库存");
        }

        operationLogger.log(adminId, "PRODUCT_CREATE", "PRODUCT", String.valueOf(product.getId()), product.getName());
        return toDetail(product);
    }

    @Transactional
    public AdminProductDetailVO update(Long id, ProductSaveRequest request, Long adminId) {
        Product product = require(id);
        Category category = requireCategory(request.categoryId());
        ProductStatus status = parseProductStatus(request.status() == null
                ? product.getStatus() : request.status());
        validateSkuCodes(request.skus(), id);
        if (status == ProductStatus.ON_SALE && !hasEnabledSku(request.skus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上架商品至少需要一个启用的 SKU");
        }

        applyProduct(product, request, category.getId(), status);
        productMapper.updateById(product);

        replaceImages(product.getId(), request.images());
        syncSkus(product.getId(), request.skus(), adminId);

        operationLogger.log(adminId, "PRODUCT_UPDATE", "PRODUCT", String.valueOf(id), product.getName());
        return toDetail(product);
    }

    @Transactional
    public void updateStatus(Long id, String statusValue, Long adminId) {
        Product product = require(id);
        ProductStatus status = parseProductStatus(statusValue);
        if (status == ProductStatus.ON_SALE) {
            long enabled = productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, id)
                    .eq(ProductSku::getStatus, SkuStatus.ENABLED.name()));
            if (enabled == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "商品没有启用的 SKU，无法上架");
            }
        }
        product.setStatus(status.name());
        productMapper.updateById(product);
        operationLogger.log(adminId, "PRODUCT_STATUS", "PRODUCT", String.valueOf(id), status.name());
    }

    @Transactional
    public void updateStock(Long skuId, Integer stock, Long adminId) {
        if (stock == null || stock < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "库存不能为负");
        }
        stockService.adjust(skuId, stock, adminId, "后台调整库存");
        operationLogger.log(adminId, "SKU_STOCK", "SKU", String.valueOf(skuId), String.valueOf(stock));
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        Product product = require(id);
        if (orderItemMapper.selectCount(new LambdaQueryWrapper<MallOrderItem>()
                .eq(MallOrderItem::getProductId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已被订单引用，不能删除，请改为下架");
        }
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        productMapper.deleteById(id);
        operationLogger.log(adminId, "PRODUCT_DELETE", "PRODUCT", String.valueOf(id), product.getName());
    }

    private void syncSkus(Long productId, List<ProductSaveRequest.SkuSaveItem> items, Long adminId) {
        Map<Long, ProductSku> existing = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId))
                .stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<Long> retainedIds = new HashSet<>();

        for (ProductSaveRequest.SkuSaveItem item : items) {
            if (item.id() != null) {
                ProductSku sku = existing.get(item.id());
                if (sku == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 不属于当前商品");
                }
                int before = sku.getStock() == null ? 0 : sku.getStock();
                applySku(sku, item);
                productSkuMapper.updateById(sku);
                stockService.recordProductChange(sku, before,
                        sku.getStock() == null ? 0 : sku.getStock(),
                        StockChangeType.MANUAL_ADJUST, adminId, "编辑商品调整库存");
                retainedIds.add(sku.getId());
            } else {
                ProductSku sku = insertSku(productId, item);
                stockService.recordProductChange(sku, 0,
                        sku.getStock() == null ? 0 : sku.getStock(),
                        StockChangeType.PRODUCT_INIT, adminId, "新增 SKU 初始库存");
            }
        }

        for (ProductSku sku : existing.values()) {
            if (retainedIds.contains(sku.getId())) {
                continue;
            }
            long referenced = orderItemMapper.selectCount(new LambdaQueryWrapper<MallOrderItem>()
                    .eq(MallOrderItem::getSkuId, sku.getId()));
            if (referenced > 0) {
                sku.setStatus(SkuStatus.DISABLED.name());
                productSkuMapper.updateById(sku);
            } else {
                productSkuMapper.deleteById(sku.getId());
            }
        }
    }

    private void replaceImages(Long productId, List<ProductSaveRequest.ProductImageItem> images) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (images == null) {
            return;
        }
        int index = 0;
        for (ProductSaveRequest.ProductImageItem image : images) {
            ProductImage entity = new ProductImage();
            entity.setProductId(productId);
            entity.setImageUrl(image.url().trim());
            entity.setSortOrder(image.sortOrder() == null ? index : image.sortOrder());
            productImageMapper.insert(entity);
            index++;
        }
    }

    private void applyProduct(Product product, ProductSaveRequest request, Long categoryId, ProductStatus status) {
        product.setCategoryId(categoryId);
        product.setName(request.name().trim());
        product.setSubtitle(request.subtitle() == null ? "" : request.subtitle().trim());
        product.setDescription(request.description() == null ? "" : request.description());
        product.setMainImage(request.mainImage() == null ? "" : request.mainImage().trim());
        product.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        product.setStatus(status.name());
        product.setShippingTemplateId(request.shippingTemplateId());
    }

    private ProductSku insertSku(Long productId, ProductSaveRequest.SkuSaveItem item) {
        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        applySku(sku, item);
        productSkuMapper.insert(sku);
        return sku;
    }

    private void applySku(ProductSku sku, ProductSaveRequest.SkuSaveItem item) {
        sku.setSkuCode(item.skuCode().trim());
        sku.setSpecJson(toSpecJson(item.spec()));
        sku.setPrice(item.price().setScale(2, RoundingMode.HALF_UP));
        sku.setStock(item.stock());
        sku.setStatus(parseSkuStatus(item.status() == null ? SkuStatus.ENABLED.name() : item.status()).name());
    }

    private String toSpecJson(Map<String, String> spec) {
        Map<String, String> normalized = spec == null ? Map.of() : spec.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null && !entry.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right));
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 规格格式不合法");
        }
    }

    private void validateSkuCodes(List<ProductSaveRequest.SkuSaveItem> skus, Long productId) {
        Set<String> codes = new HashSet<>();
        for (ProductSaveRequest.SkuSaveItem item : skus) {
            String code = item.skuCode().trim();
            if (!codes.add(code)) {
                throw new BusinessException(ErrorCode.CONFLICT, "SKU 编码重复：" + code);
            }
            LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSkuCode, code);
            if (item.id() != null) {
                wrapper.ne(ProductSku::getId, item.id());
            }
            if (productSkuMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "SKU 编码已存在：" + code);
            }
        }
        if (productId != null) {
            Map<Long, ProductSku> existing = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                            .eq(ProductSku::getProductId, productId))
                    .stream()
                    .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
            for (ProductSaveRequest.SkuSaveItem item : skus) {
                if (item.id() != null && !existing.containsKey(item.id())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 不属于当前商品");
                }
            }
        }
    }

    private boolean hasEnabledSku(List<ProductSaveRequest.SkuSaveItem> skus) {
        return skus.stream().anyMatch(item -> SkuStatus.ENABLED.name()
                .equals(parseSkuStatus(item.status() == null ? SkuStatus.ENABLED.name() : item.status()).name()));
    }

    private AdminProductDetailVO toDetail(Product product) {
        Category category = categoryMapper.selectById(product.getCategoryId());
        List<ProductImageVO> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, product.getId())
                        .orderByAsc(ProductImage::getSortOrder)
                        .orderByAsc(ProductImage::getId))
                .stream()
                .map(image -> new ProductImageVO(image.getId(), image.getImageUrl(), image.getSortOrder()))
                .toList();
        List<AdminProductDetailVO.AdminSkuVO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId())
                        .orderByAsc(ProductSku::getId))
                .stream()
                .map(sku -> new AdminProductDetailVO.AdminSkuVO(
                        sku.getId(),
                        sku.getSkuCode(),
                        specJsonParser.parse(sku.getSpecJson()),
                        sku.getPrice(),
                        sku.getStock() == null ? 0 : sku.getStock(),
                        sku.getStatus()))
                .toList();
        return new AdminProductDetailVO(
                product.getId(),
                product.getCategoryId(),
                category == null ? "" : category.getName(),
                product.getName(),
                product.getSubtitle(),
                product.getDescription(),
                product.getMainImage(),
                product.getStatus(),
                product.getSortOrder(),
                product.getShippingTemplateId(),
                images,
                skus);
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private Product require(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品状态不合法");
        }
    }

    private SkuStatus parseSkuStatus(String status) {
        try {
            return SkuStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 状态不合法");
        }
    }
}
