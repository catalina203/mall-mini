package com.mallmini.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.config.AppProperties;
import com.mallmini.inventory.entity.StockLog;
import com.mallmini.inventory.enums.StockChangeType;
import com.mallmini.inventory.mapper.StockLogMapper;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private StockLogMapper stockLogMapper;
    @Mock
    private NotificationService notificationService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getStock().setAlertThreshold(10);
        stockService = new StockService(
                productSkuMapper,
                productMapper,
                stockLogMapper,
                new SpecJsonParser(new ObjectMapper()),
                notificationService,
                properties);
    }

    @Test
    void deductWritesExactBeforeAndAfter() {
        when(productSkuMapper.selectForUpdate(1L)).thenReturn(sku(1L, 10, "ENABLED"));

        stockService.deduct(1L, 3, "ORDER", "MM-1", "USER", 7L, "下单扣减");

        verify(productSkuMapper).updateStock(1L, 7);
        ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
        verify(stockLogMapper).insert(captor.capture());
        StockLog log = captor.getValue();
        assertThat(log.getChangeType()).isEqualTo(StockChangeType.ORDER_DEDUCT.name());
        assertThat(log.getStockBefore()).isEqualTo(10);
        assertThat(log.getStockAfter()).isEqualTo(7);
        assertThat(log.getQuantityDelta()).isEqualTo(-3);
        assertThat(log.getBizNo()).isEqualTo("MM-1");
    }

    @Test
    void deductRejectsInsufficientStockWithoutUpdating() {
        when(productSkuMapper.selectForUpdate(1L)).thenReturn(sku(1L, 1, "ENABLED"));

        assertThatThrownBy(() -> stockService.deduct(
                1L, 2, "ORDER", "MM-1", "USER", 7L, "下单扣减"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
        verify(productSkuMapper, never()).updateStock(anyLong(), anyInt());
        verify(stockLogMapper, never()).insert(any(StockLog.class));
    }

    @Test
    void restoreIsIdempotentByBusinessKey() {
        when(stockLogMapper.selectCount(any())).thenReturn(1L);

        StockLog result = stockService.restore(
                1L, 2, StockChangeType.ORDER_CANCEL_RESTORE,
                "ORDER", "MM-1", "USER", 7L, "取消回补");

        assertThat(result).isNull();
        verify(productSkuMapper, never()).selectForUpdate(anyLong());
        verify(productSkuMapper, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void adjustWritesManualLogAndNotifiesWhenCrossingThreshold() {
        when(productSkuMapper.selectForUpdate(1L)).thenReturn(sku(1L, 12, "ENABLED"));

        stockService.adjust(1L, 8, 99L, "盘点");

        verify(productSkuMapper).updateStock(1L, 8);
        ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
        verify(stockLogMapper).insert(captor.capture());
        StockLog log = captor.getValue();
        assertThat(log.getChangeType()).isEqualTo(StockChangeType.MANUAL_ADJUST.name());
        assertThat(log.getQuantityDelta()).isEqualTo(-4);
        assertThat(log.getStockBefore()).isEqualTo(12);
        assertThat(log.getStockAfter()).isEqualTo(8);
        verify(notificationService).notifyAdmins(
                org.mockito.ArgumentMatchers.eq("STOCK_LOW"),
                any(), any(), any(), any(), any());
    }

    private ProductSku sku(Long id, int stock, String status) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(10L);
        sku.setSkuCode("SKU-1");
        sku.setStock(stock);
        sku.setStatus(status);
        return sku;
    }
}
