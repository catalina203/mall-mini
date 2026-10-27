package com.mallmini.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mall_order")
public class MallOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String status;

    private String paymentStatus;

    private BigDecimal goodsAmount;

    private BigDecimal discountAmount;

    private BigDecimal shippingFee;

    private BigDecimal payableAmount;

    private Long couponId;

    private String couponName;

    private String receiverName;

    private String receiverPhone;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverDetail;

    private String clientRequestNo;

    private String remark;

    private String adminRemark;

    private Boolean invoiceRequired;

    private String invoiceTitle;

    private String invoiceTaxNo;

    private String invoiceEmail;

    private String logisticsCompany;

    private String logisticsNo;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
