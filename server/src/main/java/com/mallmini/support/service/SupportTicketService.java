package com.mallmini.support.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.support.dto.CreateTicketRequest;
import com.mallmini.support.entity.SupportTicket;
import com.mallmini.support.enums.SupportStatus;
import com.mallmini.support.mapper.SupportTicketMapper;
import com.mallmini.support.vo.SupportTicketVO;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private static final Set<String> TYPES = Set.of("ORDER", "PRODUCT", "REFUND", "OTHER");

    private final SupportTicketMapper ticketMapper;
    private final MallOrderMapper orderMapper;
    private final UserMapper userMapper;
    private final AdminOperationLogger operationLogger;

    @Transactional
    public SupportTicketVO create(Long userId, CreateTicketRequest request) {
        String orderNo = request.orderNo() == null || request.orderNo().isBlank()
                ? null : request.orderNo().trim();
        if (orderNo != null) {
            MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                    .eq(MallOrder::getOrderNo, orderNo)
                    .eq(MallOrder::getUserId, userId));
            if (order == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
            }
        }
        String type = request.type().trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "问题类型不合法");
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TK" + IdWorker.getIdStr());
        ticket.setUserId(userId);
        ticket.setOrderNo(orderNo);
        ticket.setType(type);
        ticket.setSubject(request.subject().trim());
        ticket.setContent(request.content().trim());
        ticket.setStatus(SupportStatus.OPEN.name());
        ticket.setReply("");
        ticketMapper.insert(ticket);
        return toVO(ticket, userMapper.selectById(userId));
    }

    public PageResult<SupportTicketVO> listForUser(Long userId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<SupportTicket> result = ticketMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<SupportTicket>()
                        .eq(SupportTicket::getUserId, userId)
                        .orderByDesc(SupportTicket::getCreatedAt)
                        .orderByDesc(SupportTicket::getId));
        return toPage(result, safePage, safePageSize);
    }

    public SupportTicketVO detailForUser(Long userId, String ticketNo) {
        SupportTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
                .eq(SupportTicket::getTicketNo, ticketNo)
                .eq(SupportTicket::getUserId, userId));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return toVO(ticket, userMapper.selectById(userId));
    }

    public PageResult<SupportTicketVO> adminList(String statusValue,
                                                  String typeValue,
                                                  String keyword,
                                                  long page,
                                                  long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<SupportTicket> wrapper = new LambdaQueryWrapper<SupportTicket>()
                .orderByDesc(SupportTicket::getCreatedAt)
                .orderByDesc(SupportTicket::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(SupportTicket::getStatus, parseStatus(statusValue).name());
        }
        if (typeValue != null && !typeValue.isBlank()) {
            wrapper.eq(SupportTicket::getType, typeValue.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(condition -> condition
                    .like(SupportTicket::getTicketNo, normalized)
                    .or()
                    .like(SupportTicket::getSubject, normalized)
                    .or()
                    .like(SupportTicket::getOrderNo, normalized));
        }
        Page<SupportTicket> result = ticketMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        return toPage(result, safePage, safePageSize);
    }

    @Transactional
    public SupportTicketVO reply(Long adminId, String ticketNo, String reply) {
        SupportTicket ticket = requireByNo(ticketNo);
        ticket.setReply(reply.trim());
        ticket.setStatus(SupportStatus.RESOLVED.name());
        ticket.setRepliedBy(adminId);
        ticket.setRepliedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        operationLogger.log(adminId, "SUPPORT_REPLY", "SUPPORT", ticketNo, reply);
        return toVO(ticket, userMapper.selectById(ticket.getUserId()));
    }

    @Transactional
    public void closeForUser(Long userId, String ticketNo) {
        SupportTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
                .eq(SupportTicket::getTicketNo, ticketNo)
                .eq(SupportTicket::getUserId, userId));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        ticket.setStatus(SupportStatus.CLOSED.name());
        ticket.setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void closeForAdmin(Long adminId, String ticketNo) {
        SupportTicket ticket = requireByNo(ticketNo);
        ticket.setStatus(SupportStatus.CLOSED.name());
        ticket.setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        operationLogger.log(adminId, "SUPPORT_CLOSE", "SUPPORT", ticketNo, "");
    }

    private PageResult<SupportTicketVO> toPage(Page<SupportTicket> result,
                                               long page,
                                               long pageSize) {
        List<SupportTicket> tickets = result.getRecords();
        if (tickets.isEmpty()) {
            return PageResult.of(List.of(), page, pageSize, result.getTotal());
        }
        Map<Long, User> users = userMapper.selectBatchIds(
                        tickets.stream().map(SupportTicket::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<SupportTicketVO> items = tickets.stream()
                .map(ticket -> toVO(ticket, users.get(ticket.getUserId())))
                .toList();
        return PageResult.of(items, page, pageSize, result.getTotal());
    }

    private SupportTicketVO toVO(SupportTicket ticket, User user) {
        return new SupportTicketVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getUserId(),
                user == null || user.getNickname() == null ? "用户" : user.getNickname(),
                ticket.getOrderNo(),
                ticket.getType(),
                ticket.getSubject(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getReply() == null ? "" : ticket.getReply(),
                ticket.getRepliedAt(),
                ticket.getCreatedAt());
    }

    private SupportTicket requireByNo(String ticketNo) {
        SupportTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
                .eq(SupportTicket::getTicketNo, ticketNo));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private SupportStatus parseStatus(String value) {
        try {
            return SupportStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "工单状态不合法");
        }
    }
}
