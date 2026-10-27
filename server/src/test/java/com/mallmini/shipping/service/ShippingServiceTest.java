package com.mallmini.shipping.service;

import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.config.AppProperties;
import com.mallmini.product.entity.Product;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.shipping.entity.ShippingTemplate;
import com.mallmini.shipping.entity.ShippingTemplateRule;
import com.mallmini.shipping.mapper.ShippingTemplateMapper;
import com.mallmini.shipping.mapper.ShippingTemplateRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingTemplateMapper templateMapper;
    @Mock
    private ShippingTemplateRuleMapper ruleMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private AdminOperationLogger operationLogger;

    private ShippingService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getShipping().setFee(new BigDecimal("10.00"));
        properties.getShipping().setFreeThreshold(new BigDecimal("99.00"));
        service = new ShippingService(templateMapper, ruleMapper, productMapper,
                operationLogger, properties);
    }

    @Test
    void usesRegionRuleAndFreeThreshold() {
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(1L, 1L)));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(template()));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule("广东省", "20.00", "500.00")));

        BigDecimal fee = service.calculate("广东省",
                List.of(new ShippingService.ShippingLine(1L, 1, new BigDecimal("199.00"))));

        assertThat(fee).isEqualByComparingTo("20.00");
    }

    @Test
    void freeWhenGoodsReachThreshold() {
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(1L, 1L)));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(template()));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule("广东省", "20.00", "500.00")));

        BigDecimal fee = service.calculate("广东省",
                List.of(new ShippingService.ShippingLine(1L, 1, new BigDecimal("600.00"))));

        assertThat(fee).isEqualByComparingTo("0.00");
    }

    @Test
    void fallsBackToGlobalConfigWithoutTemplate() {
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(1L, null)));

        BigDecimal fee = service.calculate("广东省",
                List.of(new ShippingService.ShippingLine(1L, 1, new BigDecimal("50.00"))));

        assertThat(fee).isEqualByComparingTo("10.00");
    }

    private Product product(Long id, Long templateId) {
        Product product = new Product();
        product.setId(id);
        product.setShippingTemplateId(templateId);
        return product;
    }

    private ShippingTemplate template() {
        ShippingTemplate template = new ShippingTemplate();
        template.setId(1L);
        template.setName("测试模板");
        template.setStatus("ENABLED");
        template.setDefaultFee(new BigDecimal("10.00"));
        template.setFreeThreshold(new BigDecimal("99.00"));
        return template;
    }

    private ShippingTemplateRule rule(String region, String fee, String freeThreshold) {
        ShippingTemplateRule rule = new ShippingTemplateRule();
        rule.setTemplateId(1L);
        rule.setRegionCode(region);
        rule.setFirstUnit(1);
        rule.setFirstFee(new BigDecimal(fee));
        rule.setAdditionalUnit(1);
        rule.setAdditionalFee(BigDecimal.ZERO);
        rule.setFreeThreshold(new BigDecimal(freeThreshold));
        rule.setStatus("ENABLED");
        rule.setSortOrder(0);
        return rule;
    }
}
