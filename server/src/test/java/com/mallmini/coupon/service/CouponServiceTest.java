package com.mallmini.coupon.service;

import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.coupon.entity.Coupon;
import com.mallmini.coupon.entity.CouponTemplate;
import com.mallmini.coupon.mapper.CouponMapper;
import com.mallmini.coupon.mapper.CouponTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateMapper templateMapper;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private AdminOperationLogger operationLogger;

    private CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(templateMapper, couponMapper, operationLogger);
    }

    @Test
    void claimCreatesUnusedCoupon() {
        when(templateMapper.selectById(1L)).thenReturn(template("FIXED", "10.00", "50.00"));
        when(couponMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.claimOne(1L)).thenReturn(1);
        when(couponMapper.insert(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            coupon.setId(10L);
            return 1;
        });

        var result = service.claim(7L, 1L);

        assertThat(result.status()).isEqualTo("UNUSED");
        assertThat(result.name()).isEqualTo("测试券");
        verify(couponMapper).insert(any(Coupon.class));
    }

    @Test
    void claimRejectsDuplicate() {
        when(templateMapper.selectById(1L)).thenReturn(template("FIXED", "10.00", "50.00"));
        when(couponMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.claim(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已领取");
        verify(templateMapper, never()).claimOne(any());
    }

    @Test
    void calculateFixedDiscount() {
        when(couponMapper.selectOne(any())).thenReturn(coupon());
        when(templateMapper.selectById(1L)).thenReturn(template("FIXED", "10.00", "50.00"));

        var result = service.calculate(7L, 1L,
                List.of(new CouponService.CouponLine(1L, 10L, new BigDecimal("100.00"))),
                new BigDecimal("100.00"));

        assertThat(result.discount()).isEqualByComparingTo("10.00");
        assertThat(result.couponName()).isEqualTo("测试券");
    }

    @Test
    void calculatePercentDiscount() {
        when(couponMapper.selectOne(any())).thenReturn(coupon());
        when(templateMapper.selectById(1L)).thenReturn(template("PERCENT", "10.00", "50.00"));

        var result = service.calculate(7L, 1L,
                List.of(new CouponService.CouponLine(1L, 10L, new BigDecimal("100.00"))),
                new BigDecimal("100.00"));

        assertThat(result.discount()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculateRejectsBelowThreshold() {
        when(couponMapper.selectOne(any())).thenReturn(coupon());
        when(templateMapper.selectById(1L)).thenReturn(template("FIXED", "10.00", "50.00"));

        assertThatThrownBy(() -> service.calculate(7L, 1L,
                List.of(new CouponService.CouponLine(1L, 10L, new BigDecimal("40.00"))),
                new BigDecimal("40.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("门槛");
    }

    private CouponTemplate template(String type, String value, String minAmount) {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setName("测试券");
        template.setType(type);
        template.setValue(new BigDecimal(value));
        template.setMinAmount(new BigDecimal(minAmount));
        template.setScope("ALL");
        template.setStatus("ENABLED");
        template.setTotalCount(100);
        template.setClaimedCount(0);
        template.setPerUserLimit(1);
        template.setValidFrom(LocalDateTime.now().minusDays(1));
        template.setValidTo(LocalDateTime.now().plusDays(1));
        return template;
    }

    private Coupon coupon() {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setTemplateId(1L);
        coupon.setUserId(7L);
        coupon.setStatus("UNUSED");
        coupon.setValidFrom(LocalDateTime.now().minusDays(1));
        coupon.setValidTo(LocalDateTime.now().plusDays(1));
        return coupon;
    }
}
