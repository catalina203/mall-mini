package com.mallmini.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.config.AppProperties;
import com.mallmini.inventory.entity.StockLog;
import com.mallmini.inventory.enums.StockChangeType;
import com.mallmini.inventory.mapper.StockLogMapper;
import com.mallmini.inventory.vo.StockAlertVO;
import com.mallmini.inventory.vo.StockLogVO;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.enums.SkuStatus;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final StockLogMapper stockLogMapper;
    private final SpecJsonParser specJsonParser;
    private final NotificationService notificationService;
    private final AppProperties properties;

    @Transactional
    public StockLog deduct(Long skuId,
                           int quantity,
                           String bizType,
                           String bizNo,
                           String operatorType,
                           Long operatorId,
                           String remark) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "扣减数量必须大于 0");
        }
        ProductSku sku = productSkuMapper.selectForUpdate(skuId);
        if (sku == null || !SkuStatus.ENABLED.name().equals(sku.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品规格不存在或已停用");
        }
        int before = stockOf(sku);
        if (before < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        }
        int after = before - quantity;
        productSkuMapper.updateStock(skuId, after);
        return writeLog(sku, StockChangeType.ORDER_DEDUCT, -quantity, before, after,
                bizType, bizNo, operatorType, operatorId, remark);
    }

    @Transactional
    public StockLog restore(Long skuId,
                            int quantity,
                            StockChangeType changeType,
                            String bizType,
                            String bizNo,
                            String operatorType,
                            Long operatorId,
                            String remark) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "回补数量必须大于 0");
        }
        if (alreadyRecorded(changeType, bizNo, skuId)) {
            return null;
        }
        ProductSku sku = productSkuMapper.selectForUpdate(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        int before = stockOf(sku);
        int after = before + quantity;
        productSkuMapper.updateStock(skuId, after);
        return writeLog(sku, changeType, quantity, before, after,
                bizType, bizNo, operatorType, operatorId, remark);
    }

    @Transactional
    public StockLog adjust(Long skuId, int stock, Long adminId, String remark) {
        if (stock < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "库存不能为负");
        }
        ProductSku sku = productSkuMapper.selectForUpdate(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        int before = stockOf(sku);
        if (before == stock) {
            return null;
        }
        productSkuMapper.updateStock(skuId, stock);
        return writeLog(sku, StockChangeType.MANUAL_ADJUST, stock - before, before, stock,
                "ADMIN", "ADJ" + IdWorker.getIdStr(), "ADMIN", adminId,
                remark == null || remark.isBlank() ? "后台调整库存" : remark.trim());
    }

    public void recordProductChange(ProductSku sku,
                                    int before,
                                    int after,
                                    StockChangeType changeType,
                                    Long adminId,
                                    String remark) {
        if (sku == null || sku.getId() == null || before == after) {
            return;
        }
        writeLog(sku, changeType, after - before, before, after,
                "PRODUCT", "PRD" + sku.getId() + "-" + IdWorker.getIdStr(),
                "ADMIN", adminId, remark);
    }

    public PageResult<StockLogVO> page(Long skuId,
                                       String bizNo,
                                       String changeType,
                                       long page,
                                       long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<StockLog> wrapper = new LambdaQueryWrapper<StockLog>()
                .orderByDesc(StockLog::getCreatedAt)
                .orderByDesc(StockLog::getId);
        if (skuId != null) {
            wrapper.eq(StockLog::getSkuId, skuId);
        }
        if (bizNo != null && !bizNo.isBlank()) {
            wrapper.like(StockLog::getBizNo, bizNo.trim());
        }
        if (changeType != null && !changeType.isBlank()) {
            wrapper.eq(StockLog::getChangeType, changeType.trim().toUpperCase());
        }
        Page<StockLog> logPage = stockLogMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        List<StockLog> logs = logPage.getRecords();
        if (logs.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, logPage.getTotal());
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        logs.stream().map(StockLog::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ProductSku> skus = productSkuMapper.selectBatchIds(
                        logs.stream().map(StockLog::getSkuId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));

        List<StockLogVO> items = logs.stream().map(log -> {
            Product product = products.get(log.getProductId());
            ProductSku sku = skus.get(log.getSkuId());
            return new StockLogVO(
                    log.getId(),
                    log.getSkuId(),
                    log.getProductId(),
                    sku == null ? "" : sku.getSkuCode(),
                    product == null ? "" : product.getName(),
                    log.getChangeType(),
                    log.getQuantityDelta() == null ? 0 : log.getQuantityDelta(),
                    log.getStockBefore() == null ? 0 : log.getStockBefore(),
                    log.getStockAfter() == null ? 0 : log.getStockAfter(),
                    log.getBizType(),
                    log.getBizNo(),
                    log.getOperatorType(),
                    log.getOperatorId(),
                    log.getRemark(),
                    log.getCreatedAt());
        }).toList();
        return PageResult.of(items, safePage, safePageSize, logPage.getTotal());
    }

    public List<StockLogVO> exportList(Long skuId,
                                       String bizNo,
                                       String changeType,
                                       int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20000);
        LambdaQueryWrapper<StockLog> wrapper = new LambdaQueryWrapper<StockLog>()
                .orderByDesc(StockLog::getCreatedAt)
                .orderByDesc(StockLog::getId);
        if (skuId != null) {
            wrapper.eq(StockLog::getSkuId, skuId);
        }
        if (bizNo != null && !bizNo.isBlank()) {
            wrapper.like(StockLog::getBizNo, bizNo.trim());
        }
        if (changeType != null && !changeType.isBlank()) {
            wrapper.eq(StockLog::getChangeType, changeType.trim().toUpperCase());
        }
        wrapper.last("LIMIT " + safeLimit);
        List<StockLog> logs = stockLogMapper.selectList(wrapper);
        if (logs.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        logs.stream().map(StockLog::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ProductSku> skus = productSkuMapper.selectBatchIds(
                        logs.stream().map(StockLog::getSkuId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        return logs.stream().map(log -> {
            Product product = products.get(log.getProductId());
            ProductSku sku = skus.get(log.getSkuId());
            return new StockLogVO(
                    log.getId(),
                    log.getSkuId(),
                    log.getProductId(),
                    sku == null ? "" : sku.getSkuCode(),
                    product == null ? "" : product.getName(),
                    log.getChangeType(),
                    log.getQuantityDelta() == null ? 0 : log.getQuantityDelta(),
                    log.getStockBefore() == null ? 0 : log.getStockBefore(),
                    log.getStockAfter() == null ? 0 : log.getStockAfter(),
                    log.getBizType(),
                    log.getBizNo(),
                    log.getOperatorType(),
                    log.getOperatorId(),
                    log.getRemark(),
                    log.getCreatedAt());
        }).toList();
    }

    public List<StockAlertVO> alerts(Integer thresholdValue) {
        int threshold = thresholdValue == null || thresholdValue < 0
                ? properties.getStock().getAlertThreshold() : thresholdValue;
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getStatus, SkuStatus.ENABLED.name())
                .le(ProductSku::getStock, threshold)
                .orderByAsc(ProductSku::getStock)
                .orderByAsc(ProductSku::getId)
                .last("LIMIT 200"));
        if (skus.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        skus.stream().map(ProductSku::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return skus.stream().map(sku -> {
            Product product = products.get(sku.getProductId());
            return new StockAlertVO(
                    sku.getId(),
                    sku.getProductId(),
                    sku.getSkuCode(),
                    product == null ? "" : product.getName(),
                    String.join(" / ", specJsonParser.parse(sku.getSpecJson()).values()),
                    stockOf(sku),
                    threshold,
                    sku.getPrice());
        }).toList();
    }

    private boolean alreadyRecorded(StockChangeType changeType, String bizNo, Long skuId) {
        if (bizNo == null || bizNo.isBlank()) {
            return false;
        }
        return stockLogMapper.selectCount(new LambdaQueryWrapper<StockLog>()
                .eq(StockLog::getChangeType, changeType.name())
                .eq(StockLog::getBizNo, bizNo)
                .eq(StockLog::getSkuId, skuId)) > 0;
    }

    private StockLog writeLog(ProductSku sku,
                              StockChangeType changeType,
                              int delta,
                              int before,
                              int after,
                              String bizType,
                              String bizNo,
                              String operatorType,
                              Long operatorId,
                              String remark) {
        StockLog log = new StockLog();
        log.setSkuId(sku.getId());
        log.setProductId(sku.getProductId());
        log.setChangeType(changeType.name());
        log.setQuantityDelta(delta);
        log.setStockBefore(before);
        log.setStockAfter(after);
        log.setBizType(bizType == null ? "" : bizType);
        log.setBizNo(bizNo == null ? "" : bizNo);
        log.setOperatorType(operatorType == null ? "SYSTEM" : operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark == null ? "" : remark);
        stockLogMapper.insert(log);

        int threshold = properties.getStock().getAlertThreshold();
        if (before > threshold && after <= threshold && after >= 0) {
            notificationService.notifyAdmins(
                    "STOCK_LOW",
                    "库存预警",
                    "SKU " + sku.getSkuCode() + " 库存已降至 " + after + "，请及时补货。",
                    "SKU",
                    String.valueOf(sku.getId()),
                    Map.of("skuId", sku.getId(), "stock", after, "threshold", threshold));
        }
        return log;
    }

    private int stockOf(ProductSku sku) {
        return sku.getStock() == null ? 0 : sku.getStock();
    }
}
