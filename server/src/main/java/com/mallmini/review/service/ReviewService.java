package com.mallmini.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.review.dto.CreateReviewRequest;
import com.mallmini.review.entity.ProductReview;
import com.mallmini.review.enums.ReviewStatus;
import com.mallmini.review.mapper.ProductReviewMapper;
import com.mallmini.review.vo.ReviewSummaryVO;
import com.mallmini.review.vo.ReviewVO;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewMapper reviewMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final AdminOperationLogger operationLogger;

    @Transactional
    public ReviewVO create(Long userId, String orderNo, CreateReviewRequest request) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单完成后才能评价");
        }
        MallOrderItem item = orderItemMapper.selectById(request.orderItemId());
        if (item == null || !order.getId().equals(item.getOrderId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单明细不属于该订单");
        }
        long existing = reviewMapper.selectCount(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getOrderItemId, request.orderItemId()));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该商品已评价");
        }
        ProductReview review = new ProductReview();
        review.setOrderNo(orderNo);
        review.setOrderItemId(item.getId());
        review.setProductId(item.getProductId());
        review.setSkuId(item.getSkuId());
        review.setUserId(userId);
        review.setRating(request.rating());
        review.setContent(request.content() == null ? "" : request.content().trim());
        review.setAnonymous(Boolean.TRUE.equals(request.anonymous()));
        review.setStatus(ReviewStatus.PENDING.name());
        review.setReply("");
        reviewMapper.insert(review);
        return toVO(review, userMapper.selectById(userId));
    }

    public PageResult<ReviewVO> listForProduct(Long productId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<ProductReview> result = reviewMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getProductId, productId)
                        .eq(ProductReview::getStatus, ReviewStatus.APPROVED.name())
                        .orderByDesc(ProductReview::getCreatedAt)
                        .orderByDesc(ProductReview::getId));
        return toPage(result, safePage, safePageSize);
    }

    public ReviewSummaryVO summary(Long productId) {
        BigDecimal average = reviewMapper.averageRating(productId);
        return new ReviewSummaryVO(
                productId,
                average == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : average.setScale(2, RoundingMode.HALF_UP),
                reviewMapper.approvedCount(productId));
    }

    public PageResult<ReviewVO> adminList(String statusValue,
                                          Long productId,
                                          long page,
                                          long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<ProductReview>()
                .orderByDesc(ProductReview::getCreatedAt)
                .orderByDesc(ProductReview::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(ProductReview::getStatus, parseStatus(statusValue).name());
        }
        if (productId != null) {
            wrapper.eq(ProductReview::getProductId, productId);
        }
        Page<ProductReview> result = reviewMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        return toPage(result, safePage, safePageSize);
    }

    @Transactional
    public ReviewVO approve(Long adminId, Long reviewId) {
        ProductReview review = require(reviewId);
        if (ReviewStatus.APPROVED.name().equals(review.getStatus())) {
            return toVO(review, userMapper.selectById(review.getUserId()));
        }
        review.setStatus(ReviewStatus.APPROVED.name());
        reviewMapper.updateById(review);
        operationLogger.log(adminId, "REVIEW_APPROVE", "REVIEW", String.valueOf(reviewId), "");
        return toVO(review, userMapper.selectById(review.getUserId()));
    }

    @Transactional
    public ReviewVO reject(Long adminId, Long reviewId) {
        ProductReview review = require(reviewId);
        review.setStatus(ReviewStatus.REJECTED.name());
        reviewMapper.updateById(review);
        operationLogger.log(adminId, "REVIEW_REJECT", "REVIEW", String.valueOf(reviewId), "");
        return toVO(review, userMapper.selectById(review.getUserId()));
    }

    @Transactional
    public ReviewVO reply(Long adminId, Long reviewId, String reply) {
        ProductReview review = require(reviewId);
        review.setReply(reply.trim());
        reviewMapper.updateById(review);
        operationLogger.log(adminId, "REVIEW_REPLY", "REVIEW", String.valueOf(reviewId), reply);
        return toVO(review, userMapper.selectById(review.getUserId()));
    }

    private PageResult<ReviewVO> toPage(Page<ProductReview> result, long page, long pageSize) {
        List<ProductReview> reviews = result.getRecords();
        if (reviews.isEmpty()) {
            return PageResult.of(List.of(), page, pageSize, result.getTotal());
        }
        Map<Long, User> users = userMapper.selectBatchIds(
                        reviews.stream().map(ProductReview::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<ReviewVO> items = reviews.stream()
                .map(review -> toVO(review, users.get(review.getUserId())))
                .toList();
        return PageResult.of(items, page, pageSize, result.getTotal());
    }

    private ReviewVO toVO(ProductReview review, User user) {
        boolean anonymous = Boolean.TRUE.equals(review.getAnonymous());
        String nickname = anonymous ? "匿名用户"
                : user == null || user.getNickname() == null || user.getNickname().isBlank()
                ? "用户" : user.getNickname();
        return new ReviewVO(
                review.getId(),
                review.getProductId(),
                review.getSkuId(),
                nickname,
                review.getRating() == null ? 0 : review.getRating(),
                review.getContent() == null ? "" : review.getContent(),
                anonymous,
                review.getStatus(),
                review.getReply() == null ? "" : review.getReply(),
                review.getCreatedAt());
    }

    private ProductReview require(Long id) {
        ProductReview review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    private ReviewStatus parseStatus(String value) {
        try {
            return ReviewStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "评价状态不合法");
        }
    }
}
