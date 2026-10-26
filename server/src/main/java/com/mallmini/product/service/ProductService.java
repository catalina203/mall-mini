package com.mallmini.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductImage;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.enums.ProductSort;
import com.mallmini.product.enums.ProductStatus;
import com.mallmini.product.enums.SkuStatus;
import com.mallmini.product.mapper.ProductImageMapper;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.vo.ProductDetailVO;
import com.mallmini.product.vo.ProductImageVO;
import com.mallmini.product.vo.ProductSummaryVO;
import com.mallmini.product.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryService categoryService;
    private final SpecJsonParser specJsonParser;

    public PageResult<ProductSummaryVO> listOnSale(long page,
                                                   long pageSize,
                                                   Long categoryId,
                                                   String keyword,
                                                   String sortValue,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "最低价不能高于最高价");
        }
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        ProductSort sort = ProductSort.from(sortValue);
        List<Long> categoryIds = categoryId == null ? null : categoryService.enabledCategoryIds(categoryId);

        IPage<Product> productPage = productMapper.selectOnSalePage(
                new Page<>(safePage, safePageSize),
                categoryIds,
                normalizedKeyword,
                minPrice,
                maxPrice,
                sort.value());

        List<Product> products = productPage.getRecords();
        if (products.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, productPage.getTotal());
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductSku>> skusByProduct = loadEnabledSkus(productIds);

        List<ProductSummaryVO> items = new ArrayList<>();
        for (Product product : products) {
            List<ProductSku> skus = skusByProduct.getOrDefault(product.getId(), List.of());
            if (skus.isEmpty()) {
                continue;
            }
            BigDecimal min = skus.stream().map(ProductSku::getPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal max = skus.stream().map(ProductSku::getPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            int totalStock = skus.stream().mapToInt(sku -> Math.max(sku.getStock(), 0)).sum();
            items.add(new ProductSummaryVO(
                    product.getId(),
                    product.getName(),
                    product.getSubtitle(),
                    product.getMainImage(),
                    min,
                    max,
                    totalStock));
        }
        return PageResult.of(items, safePage, safePageSize, productPage.getTotal());
    }

    public List<ProductSummaryVO> recommended(int limit) {
        return listOnSale(1, limit, null, null, ProductSort.NEW.value(), null, null).items();
    }

    public ProductDetailVO getDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null
                || !ProductStatus.ON_SALE.name().equals(product.getStatus())
                || !categoryService.isEnabled(product.getCategoryId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }

        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder)
                .orderByAsc(ProductImage::getId));
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .orderByAsc(ProductSku::getId));

        boolean hasEnabledSku = skus.stream()
                .anyMatch(sku -> SkuStatus.ENABLED.name().equals(sku.getStatus()));
        if (!hasEnabledSku) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品暂不可购买");
        }

        List<ProductImageVO> imageVOs = images.stream()
                .map(image -> new ProductImageVO(image.getId(), image.getImageUrl(), image.getSortOrder()))
                .toList();
        List<SkuVO> skuVOs = skus.stream()
                .map(sku -> new SkuVO(
                        sku.getId(),
                        sku.getSkuCode(),
                        specJsonParser.parse(sku.getSpecJson()),
                        sku.getPrice(),
                        Math.max(sku.getStock(), 0),
                        sku.getStatus()))
                .toList();

        return new ProductDetailVO(
                product.getId(),
                product.getName(),
                product.getSubtitle(),
                product.getDescription(),
                product.getMainImage(),
                product.getCategoryId(),
                product.getStatus(),
                imageVOs,
                skuVOs);
    }

    private Map<Long, List<ProductSku>> loadEnabledSkus(List<Long> productIds) {
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .in(ProductSku::getProductId, productIds)
                .eq(ProductSku::getStatus, SkuStatus.ENABLED.name())
                .orderByAsc(ProductSku::getId));
        return skus.stream().collect(Collectors.groupingBy(ProductSku::getProductId, LinkedHashMap::new, Collectors.toList()));
    }

}
