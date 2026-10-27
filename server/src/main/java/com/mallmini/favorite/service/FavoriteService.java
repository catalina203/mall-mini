package com.mallmini.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.favorite.entity.BrowseHistory;
import com.mallmini.favorite.entity.Favorite;
import com.mallmini.favorite.mapper.BrowseHistoryMapper;
import com.mallmini.favorite.mapper.FavoriteMapper;
import com.mallmini.favorite.vo.FavoriteProductVO;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BrowseHistoryMapper historyMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Transactional
    public void addFavorite(Long userId, Long productId) {
        requireProduct(productId);
        if (favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ignored) {
            // 并发重复收藏按已收藏处理
        }
    }

    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    public boolean isFavorite(Long userId, Long productId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    public PageResult<FavoriteProductVO> favorites(Long userId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<Favorite> result = favoriteMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt)
                        .orderByDesc(Favorite::getId));
        return toPage(result.getRecords().stream().map(favorite ->
                        new Row(favorite.getProductId(), favorite.getCreatedAt())).toList(),
                result.getTotal(), safePage, safePageSize);
    }

    @Transactional
    public void recordHistory(Long userId, Long productId) {
        requireProduct(productId);
        BrowseHistory existing = historyMapper.selectOne(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getProductId, productId));
        if (existing == null) {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setProductId(productId);
            history.setViewedAt(LocalDateTime.now());
            try {
                historyMapper.insert(history);
            } catch (DuplicateKeyException ignored) {
                // 并发写入按已记录处理
            }
        } else {
            existing.setViewedAt(LocalDateTime.now());
            historyMapper.updateById(existing);
        }
    }

    public PageResult<FavoriteProductVO> history(Long userId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<BrowseHistory> result = historyMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId)
                        .orderByDesc(BrowseHistory::getViewedAt)
                        .orderByDesc(BrowseHistory::getId));
        return toPage(result.getRecords().stream().map(history ->
                        new Row(history.getProductId(), history.getViewedAt())).toList(),
                result.getTotal(), safePage, safePageSize);
    }

    @Transactional
    public void clearHistory(Long userId) {
        historyMapper.delete(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId));
    }

    private PageResult<FavoriteProductVO> toPage(List<Row> rows,
                                                 long total,
                                                 long page,
                                                 long pageSize) {
        if (rows.isEmpty()) {
            return PageResult.of(List.of(), page, pageSize, total);
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        rows.stream().map(Row::productId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, List<ProductSku>> skus = productSkuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>()
                                .in(ProductSku::getProductId,
                                        rows.stream().map(Row::productId).distinct().toList()))
                .stream().collect(Collectors.groupingBy(ProductSku::getProductId));
        List<FavoriteProductVO> items = rows.stream().map(row -> {
            Product product = products.get(row.productId);
            if (product == null) {
                return new FavoriteProductVO(row.productId, "商品已删除", "", "",
                        BigDecimal.ZERO, "OFF_SALE", row.at);
            }
            BigDecimal minPrice = skus.getOrDefault(product.getId(), List.of()).stream()
                    .map(ProductSku::getPrice)
                    .filter(price -> price != null)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            return new FavoriteProductVO(
                    product.getId(),
                    product.getName(),
                    product.getSubtitle(),
                    product.getMainImage(),
                    minPrice,
                    product.getStatus(),
                    row.at);
        }).toList();
        return PageResult.of(items, page, pageSize, total);
    }

    private void requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
    }

    private record Row(Long productId, LocalDateTime at) {
    }
}
