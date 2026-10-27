package com.mallmini.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.coupon.dto.CouponTemplateSaveRequest;
import com.mallmini.coupon.entity.Coupon;
import com.mallmini.coupon.entity.CouponTemplate;
import com.mallmini.coupon.enums.CouponScope;
import com.mallmini.coupon.enums.CouponStatus;
import com.mallmini.coupon.enums.CouponType;
import com.mallmini.coupon.mapper.CouponMapper;
import com.mallmini.coupon.mapper.CouponTemplateMapper;
import com.mallmini.coupon.vo.CouponTemplateVO;
import com.mallmini.coupon.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponTemplateMapper templateMapper;
    private final CouponMapper couponMapper;
    private final AdminOperationLogger operationLogger;

    public List<CouponTemplateVO> templates() {
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                        .orderByDesc(CouponTemplate::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public CouponTemplateVO create(CouponTemplateSaveRequest request, Long adminId) {
        CouponTemplate template = new CouponTemplate();
        apply(template, request);
        template.setClaimedCount(0);
        templateMapper.insert(template);
        operationLogger.log(adminId, "COUPON_CREATE", "COUPON", String.valueOf(template.getId()),
                template.getName());
        return toVO(template);
    }

    @Transactional
    public CouponTemplateVO update(Long id, CouponTemplateSaveRequest request, Long adminId) {
        CouponTemplate template = requireTemplate(id);
        apply(template, request);
        templateMapper.updateById(template);
        operationLogger.log(adminId, "COUPON_UPDATE", "COUPON", String.valueOf(id), template.getName());
        return toVO(template);
    }

    @Transactional
    public void updateStatus(Long id, String statusValue, Long adminId) {
        CouponTemplate template = requireTemplate(id);
        String status = "DISABLED".equalsIgnoreCase(statusValue) ? "DISABLED" : "ENABLED";
        template.setStatus(status);
        templateMapper.updateById(template);
        operationLogger.log(adminId, "COUPON_STATUS", "COUPON", String.valueOf(id), status);
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        CouponTemplate template = requireTemplate(id);
        long claimed = couponMapper.selectCount(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getTemplateId, id));
        if (claimed > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已发放，不能删除，请改为停用");
        }
        templateMapper.deleteById(id);
        operationLogger.log(adminId, "COUPON_DELETE", "COUPON", String.valueOf(id), template.getName());
    }

    public List<CouponTemplateVO> claimable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, "ENABLED")
                .le(CouponTemplate::getValidFrom, now)
                .ge(CouponTemplate::getValidTo, now)
                .and(w -> w.eq(CouponTemplate::getTotalCount, 0)
                        .or()
                        .apply("claimed_count < total_count"))
                .orderByDesc(CouponTemplate::getId));
        if (templates.isEmpty()) {
            return List.of();
        }
        List<Long> claimedTemplateIds = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getUserId, userId)
                        .in(Coupon::getTemplateId, templates.stream().map(CouponTemplate::getId).toList()))
                .stream().map(Coupon::getTemplateId).toList();
        return templates.stream()
                .filter(template -> !claimedTemplateIds.contains(template.getId()))
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public UserCouponVO claim(Long userId, Long templateId) {
        CouponTemplate template = requireTemplate(templateId);
        LocalDateTime now = LocalDateTime.now();
        if (!"ENABLED".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已停止发放");
        }
        if (template.getValidFrom() != null && template.getValidFrom().isAfter(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券尚未开始发放");
        }
        if (template.getValidTo() != null && template.getValidTo().isBefore(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已过期");
        }
        if (template.getTotalCount() != null && template.getTotalCount() > 0
                && template.getClaimedCount() != null
                && template.getClaimedCount() >= template.getTotalCount()) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已领完");
        }
        long existing = couponMapper.selectCount(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getTemplateId, templateId)
                .eq(Coupon::getUserId, userId));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已领取过该优惠券");
        }
        int updated = templateMapper.claimOne(templateId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已领完");
        }
        Coupon coupon = new Coupon();
        coupon.setCouponNo("CP" + IdWorker.getIdStr());
        coupon.setTemplateId(templateId);
        coupon.setUserId(userId);
        coupon.setStatus(CouponStatus.UNUSED.name());
        coupon.setValidFrom(template.getValidFrom());
        coupon.setValidTo(template.getValidTo());
        try {
            couponMapper.insert(coupon);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "您已领取过该优惠券");
        }
        return toUserVO(coupon, template);
    }

    public List<UserCouponVO> myCoupons(Long userId, String statusValue) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getUserId, userId)
                .orderByAsc(Coupon::getStatus)
                .orderByAsc(Coupon::getValidTo);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(Coupon::getStatus, statusValue.trim().toUpperCase());
        }
        List<Coupon> coupons = couponMapper.selectList(wrapper);
        if (coupons.isEmpty()) {
            return List.of();
        }
        List<CouponTemplate> templates = templateMapper.selectBatchIds(
                coupons.stream().map(Coupon::getTemplateId).distinct().toList());
        java.util.Map<Long, CouponTemplate> map = templates.stream()
                .collect(java.util.stream.Collectors.toMap(CouponTemplate::getId, t -> t));
        LocalDateTime now = LocalDateTime.now();
        return coupons.stream().map(coupon -> {
            CouponTemplate template = map.get(coupon.getTemplateId());
            CouponStatus status = CouponStatus.valueOf(coupon.getStatus());
            if (status == CouponStatus.UNUSED && coupon.getValidTo() != null
                    && coupon.getValidTo().isBefore(now)) {
                status = CouponStatus.EXPIRED;
            }
            return new UserCouponVO(
                    coupon.getId(),
                    coupon.getCouponNo(),
                    coupon.getTemplateId(),
                    template == null ? "" : template.getName(),
                    template == null ? "" : template.getType(),
                    template == null ? BigDecimal.ZERO : template.getValue(),
                    template == null ? BigDecimal.ZERO : template.getMinAmount(),
                    template == null ? "ALL" : template.getScope(),
                    template == null ? null : template.getScopeId(),
                    status.name(),
                    coupon.getOrderNo(),
                    coupon.getValidFrom(),
                    coupon.getValidTo());
        }).toList();
    }

    public CouponApplication calculate(Long userId,
                                       Long couponId,
                                       List<CouponLine> lines,
                                       BigDecimal goodsAmount) {
        if (couponId == null) {
            return CouponApplication.none();
        }
        Coupon coupon = couponMapper.selectOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .eq(Coupon::getUserId, userId));
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (!CouponStatus.UNUSED.name().equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)
                || coupon.getValidTo() != null && coupon.getValidTo().isBefore(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券不在有效期内");
        }
        CouponTemplate template = requireTemplate(coupon.getTemplateId());
        if (!"ENABLED".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已停用");
        }
        BigDecimal eligible = eligibleAmount(template, lines, goodsAmount);
        if (eligible.compareTo(template.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "优惠券使用门槛为 " + template.getMinAmount().toPlainString() + " 元");
        }
        BigDecimal discount;
        if (CouponType.FIXED.name().equals(template.getType())) {
            discount = template.getValue().min(eligible);
        } else {
            discount = eligible.multiply(template.getValue())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        discount = discount.min(goodsAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new CouponApplication(coupon.getId(), template.getName(), discount);
    }

    public void lock(Long couponId, Long userId, String orderNo) {
        if (couponId == null) {
            return;
        }
        int updated = couponMapper.lockCoupon(couponId, userId, orderNo);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已被使用或已过期");
        }
    }

    public void release(String orderNo) {
        if (orderNo != null) {
            couponMapper.releaseByOrderNo(orderNo);
        }
    }

    private BigDecimal eligibleAmount(CouponTemplate template,
                                      List<CouponLine> lines,
                                      BigDecimal goodsAmount) {
        CouponScope scope = CouponScope.valueOf(template.getScope());
        if (scope == CouponScope.ALL) {
            return goodsAmount;
        }
        return lines.stream()
                .filter(line -> scope == CouponScope.CATEGORY
                        ? line.categoryId() != null && line.categoryId().equals(template.getScopeId())
                        : line.productId().equals(template.getScopeId()))
                .map(CouponLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void apply(CouponTemplate template, CouponTemplateSaveRequest request) {
        CouponType type;
        CouponScope scope;
        try {
            type = CouponType.valueOf(request.type().trim().toUpperCase());
            scope = CouponScope.valueOf(request.scope().trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "优惠券类型/范围不合法");
        }
        if (request.validFrom().isAfter(request.validTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "生效时间必须早于失效时间");
        }
        if (type == CouponType.PERCENT
                && request.value().compareTo(HUNDRED) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "折扣百分比不能超过 100");
        }
        if (scope != CouponScope.ALL && request.scopeId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择适用分类/商品");
        }
        template.setName(request.name().trim());
        template.setType(type.name());
        template.setValue(request.value().setScale(2, RoundingMode.HALF_UP));
        template.setMinAmount(request.minAmount().setScale(2, RoundingMode.HALF_UP));
        template.setScope(scope.name());
        template.setScopeId(scope == CouponScope.ALL ? null : request.scopeId());
        template.setTotalCount(request.totalCount());
        template.setPerUserLimit(request.perUserLimit());
        template.setValidFrom(request.validFrom());
        template.setValidTo(request.validTo());
        template.setStatus("DISABLED".equalsIgnoreCase(request.status()) ? "DISABLED" : "ENABLED");
    }

    private CouponTemplate requireTemplate(Long id) {
        CouponTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券模板不存在");
        }
        return template;
    }

    private CouponTemplateVO toVO(CouponTemplate template) {
        return new CouponTemplateVO(
                template.getId(),
                template.getName(),
                template.getType(),
                template.getValue(),
                template.getMinAmount(),
                template.getScope(),
                template.getScopeId(),
                template.getTotalCount() == null ? 0 : template.getTotalCount(),
                template.getClaimedCount() == null ? 0 : template.getClaimedCount(),
                template.getPerUserLimit() == null ? 1 : template.getPerUserLimit(),
                template.getValidFrom(),
                template.getValidTo(),
                template.getStatus(),
                template.getCreatedAt());
    }

    private UserCouponVO toUserVO(Coupon coupon, CouponTemplate template) {
        return new UserCouponVO(
                coupon.getId(),
                coupon.getCouponNo(),
                coupon.getTemplateId(),
                template.getName(),
                template.getType(),
                template.getValue(),
                template.getMinAmount(),
                template.getScope(),
                template.getScopeId(),
                coupon.getStatus(),
                coupon.getOrderNo(),
                coupon.getValidFrom(),
                coupon.getValidTo());
    }

    public record CouponLine(Long productId, Long categoryId, BigDecimal subtotal) {
    }

    public record CouponApplication(Long couponId, String couponName, BigDecimal discount) {
        public static CouponApplication none() {
            return new CouponApplication(null, "", BigDecimal.ZERO);
        }
    }
}
