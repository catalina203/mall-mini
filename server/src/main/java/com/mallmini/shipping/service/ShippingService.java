package com.mallmini.shipping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.config.AppProperties;
import com.mallmini.product.entity.Product;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.shipping.dto.ShippingTemplateSaveRequest;
import com.mallmini.shipping.entity.ShippingTemplate;
import com.mallmini.shipping.entity.ShippingTemplateRule;
import com.mallmini.shipping.mapper.ShippingTemplateMapper;
import com.mallmini.shipping.mapper.ShippingTemplateRuleMapper;
import com.mallmini.shipping.vo.ShippingOptionVO;
import com.mallmini.shipping.vo.ShippingTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ShippingTemplateMapper templateMapper;
    private final ShippingTemplateRuleMapper ruleMapper;
    private final ProductMapper productMapper;
    private final AdminOperationLogger operationLogger;
    private final AppProperties properties;

    public List<ShippingTemplateVO> list() {
        List<ShippingTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<ShippingTemplate>().orderByAsc(ShippingTemplate::getId));
        if (templates.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ShippingTemplateRule>> rules = ruleMapper.selectList(
                        new LambdaQueryWrapper<ShippingTemplateRule>()
                                .in(ShippingTemplateRule::getTemplateId,
                                        templates.stream().map(ShippingTemplate::getId).toList())
                                .orderByAsc(ShippingTemplateRule::getSortOrder)
                                .orderByAsc(ShippingTemplateRule::getId))
                .stream()
                .collect(Collectors.groupingBy(ShippingTemplateRule::getTemplateId));
        return templates.stream().map(template -> toVO(template,
                rules.getOrDefault(template.getId(), List.of()))).toList();
    }

    public List<ShippingOptionVO> options() {
        return templateMapper.selectList(new LambdaQueryWrapper<ShippingTemplate>()
                        .eq(ShippingTemplate::getStatus, "ENABLED")
                        .orderByAsc(ShippingTemplate::getId))
                .stream()
                .map(template -> new ShippingOptionVO(template.getId(), template.getName()))
                .toList();
    }

    @Transactional
    public ShippingTemplateVO create(ShippingTemplateSaveRequest request, Long adminId) {
        ShippingTemplate template = new ShippingTemplate();
        apply(template, request);
        templateMapper.insert(template);
        replaceRules(template.getId(), request.rules());
        operationLogger.log(adminId, "SHIPPING_CREATE", "SHIPPING", String.valueOf(template.getId()),
                template.getName());
        return toVO(template, rulesOf(template.getId()));
    }

    @Transactional
    public ShippingTemplateVO update(Long id, ShippingTemplateSaveRequest request, Long adminId) {
        ShippingTemplate template = require(id);
        apply(template, request);
        templateMapper.updateById(template);
        replaceRules(id, request.rules());
        operationLogger.log(adminId, "SHIPPING_UPDATE", "SHIPPING", String.valueOf(id),
                template.getName());
        return toVO(template, rulesOf(id));
    }

    @Transactional
    public void updateStatus(Long id, String statusValue, Long adminId) {
        ShippingTemplate template = require(id);
        String status = "DISABLED".equalsIgnoreCase(statusValue) ? "DISABLED" : "ENABLED";
        template.setStatus(status);
        templateMapper.updateById(template);
        operationLogger.log(adminId, "SHIPPING_STATUS", "SHIPPING", String.valueOf(id), status);
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        require(id);
        long used = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getShippingTemplateId, id));
        if (used > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "有商品正在使用该运费模板，不能删除");
        }
        ruleMapper.delete(new LambdaQueryWrapper<ShippingTemplateRule>()
                .eq(ShippingTemplateRule::getTemplateId, id));
        templateMapper.deleteById(id);
        operationLogger.log(adminId, "SHIPPING_DELETE", "SHIPPING", String.valueOf(id), "");
    }

    public BigDecimal calculate(String province, List<ShippingLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return ZERO;
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        lines.stream().map(ShippingLine::productId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<Long, List<ShippingLine>> groups = lines.stream().collect(Collectors.groupingBy(line -> {
            Product product = products.get(line.productId());
            return product == null || product.getShippingTemplateId() == null
                    ? -1L : product.getShippingTemplateId();
        }));

        Map<Long, ShippingTemplate> templates = groups.keySet().stream()
                .filter(id -> id > 0)
                .findAny()
                .map(ignored -> templateMapper.selectBatchIds(groups.keySet().stream()
                                .filter(id -> id > 0).toList())
                        .stream()
                        .collect(Collectors.toMap(ShippingTemplate::getId, Function.identity())))
                .orElse(Map.of());
        Map<Long, List<ShippingTemplateRule>> rules = templates.isEmpty() ? Map.of()
                : ruleMapper.selectList(new LambdaQueryWrapper<ShippingTemplateRule>()
                        .in(ShippingTemplateRule::getTemplateId, templates.keySet())
                        .eq(ShippingTemplateRule::getStatus, "ENABLED")
                        .orderByAsc(ShippingTemplateRule::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(ShippingTemplateRule::getTemplateId));

        BigDecimal total = ZERO;
        for (Map.Entry<Long, List<ShippingLine>> entry : groups.entrySet()) {
            List<ShippingLine> group = entry.getValue();
            BigDecimal goods = group.stream().map(line -> line.subtotal())
                    .reduce(ZERO, BigDecimal::add);
            int quantity = group.stream().mapToInt(ShippingLine::quantity).sum();
            total = total.add(groupFee(entry.getKey(), province, goods, quantity,
                    templates, rules));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal groupFee(Long templateId,
                                String province,
                                BigDecimal goods,
                                int quantity,
                                Map<Long, ShippingTemplate> templates,
                                Map<Long, List<ShippingTemplateRule>> rules) {
        ShippingTemplate template = templateId > 0 ? templates.get(templateId) : null;
        if (template == null || !"ENABLED".equals(template.getStatus())) {
            BigDecimal threshold = properties.getShipping().getFreeThreshold();
            if (threshold != null && threshold.signum() > 0 && goods.compareTo(threshold) >= 0) {
                return ZERO;
            }
            return properties.getShipping().getFee() == null ? ZERO
                    : properties.getShipping().getFee().setScale(2, RoundingMode.HALF_UP);
        }

        List<ShippingTemplateRule> templateRules = rules.getOrDefault(templateId, List.of());
        ShippingTemplateRule rule = templateRules.stream()
                .filter(item -> province != null && province.equals(item.getRegionCode()))
                .findFirst()
                .orElseGet(() -> templateRules.stream()
                        .filter(item -> item.getRegionCode() == null || item.getRegionCode().isBlank())
                        .findFirst().orElse(null));

        BigDecimal threshold = rule == null ? template.getFreeThreshold() : rule.getFreeThreshold();
        if (threshold != null && threshold.signum() > 0 && goods.compareTo(threshold) >= 0) {
            return ZERO;
        }
        if (rule == null) {
            return template.getDefaultFee() == null ? ZERO
                    : template.getDefaultFee().setScale(2, RoundingMode.HALF_UP);
        }
        int firstUnit = rule.getFirstUnit() == null || rule.getFirstUnit() < 1 ? 1 : rule.getFirstUnit();
        int additionalUnit = rule.getAdditionalUnit() == null || rule.getAdditionalUnit() < 1
                ? 1 : rule.getAdditionalUnit();
        BigDecimal firstFee = rule.getFirstFee() == null ? ZERO : rule.getFirstFee();
        BigDecimal additionalFee = rule.getAdditionalFee() == null ? ZERO : rule.getAdditionalFee();
        int extra = Math.max(quantity - firstUnit, 0);
        int blocks = extra == 0 ? 0 : (extra + additionalUnit - 1) / additionalUnit;
        return firstFee.add(additionalFee.multiply(BigDecimal.valueOf(blocks)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(ShippingTemplate template, ShippingTemplateSaveRequest request) {
        template.setName(request.name().trim());
        template.setDefaultFee(request.defaultFee().setScale(2, RoundingMode.HALF_UP));
        template.setFreeThreshold(request.freeThreshold().setScale(2, RoundingMode.HALF_UP));
        template.setStatus("DISABLED".equalsIgnoreCase(request.status()) ? "DISABLED" : "ENABLED");
    }

    private void replaceRules(Long templateId, List<ShippingTemplateSaveRequest.RuleItem> items) {
        ruleMapper.delete(new LambdaQueryWrapper<ShippingTemplateRule>()
                .eq(ShippingTemplateRule::getTemplateId, templateId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int index = 0;
        for (ShippingTemplateSaveRequest.RuleItem item : items) {
            ShippingTemplateRule rule = new ShippingTemplateRule();
            rule.setTemplateId(templateId);
            rule.setRegionCode(item.regionCode() == null ? "" : item.regionCode().trim());
            rule.setFirstUnit(item.firstUnit());
            rule.setFirstFee(item.firstFee().setScale(2, RoundingMode.HALF_UP));
            rule.setAdditionalUnit(item.additionalUnit());
            rule.setAdditionalFee(item.additionalFee().setScale(2, RoundingMode.HALF_UP));
            rule.setFreeThreshold(item.freeThreshold().setScale(2, RoundingMode.HALF_UP));
            rule.setStatus("DISABLED".equalsIgnoreCase(item.status()) ? "DISABLED" : "ENABLED");
            rule.setSortOrder(item.sortOrder() == null ? index : item.sortOrder());
            ruleMapper.insert(rule);
            index++;
        }
    }

    private List<ShippingTemplateRule> rulesOf(Long templateId) {
        return ruleMapper.selectList(new LambdaQueryWrapper<ShippingTemplateRule>()
                .eq(ShippingTemplateRule::getTemplateId, templateId)
                .orderByAsc(ShippingTemplateRule::getSortOrder)
                .orderByAsc(ShippingTemplateRule::getId));
    }

    private ShippingTemplate require(Long id) {
        ShippingTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "运费模板不存在");
        }
        return template;
    }

    private ShippingTemplateVO toVO(ShippingTemplate template, List<ShippingTemplateRule> rules) {
        List<ShippingTemplateVO.RuleVO> ruleVOs = rules.stream().map(rule ->
                new ShippingTemplateVO.RuleVO(
                        rule.getId(),
                        rule.getRegionCode(),
                        rule.getFirstUnit() == null ? 1 : rule.getFirstUnit(),
                        rule.getFirstFee(),
                        rule.getAdditionalUnit() == null ? 1 : rule.getAdditionalUnit(),
                        rule.getAdditionalFee(),
                        rule.getFreeThreshold(),
                        rule.getStatus(),
                        rule.getSortOrder() == null ? 0 : rule.getSortOrder())).toList();
        return new ShippingTemplateVO(
                template.getId(),
                template.getName(),
                template.getDefaultFee(),
                template.getFreeThreshold(),
                template.getStatus(),
                ruleVOs);
    }

    public record ShippingLine(Long productId, int quantity, BigDecimal subtotal) {
    }
}
