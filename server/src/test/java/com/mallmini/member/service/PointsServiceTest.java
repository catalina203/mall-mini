package com.mallmini.member.service;

import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.member.entity.PointsLog;
import com.mallmini.member.mapper.PointsLogMapper;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PointsLogMapper pointsLogMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminOperationLogger operationLogger;

    private PointsService service;

    @BeforeEach
    void setUp() {
        service = new PointsService(userMapper, pointsLogMapper, notificationService, operationLogger);
    }

    @Test
    void awardOrderPointsAddsBalanceAndLevel() {
        when(pointsLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(1L)).thenReturn(user(0, 0));
        when(pointsLogMapper.insert(any(PointsLog.class))).thenReturn(1);

        service.awardOrderPoints("MM-1", 1L, new BigDecimal("199.00"));

        verify(userMapper).updateById(any(User.class));
        verify(pointsLogMapper).insert(any(PointsLog.class));
        verify(notificationService).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void awardIsIdempotent() {
        when(pointsLogMapper.selectCount(any())).thenReturn(1L);

        service.awardOrderPoints("MM-1", 1L, new BigDecimal("199.00"));

        verify(userMapper, never()).updateById(any(User.class));
        verify(pointsLogMapper, never()).insert(any(PointsLog.class));
    }

    @Test
    void revokeOrderPointsDeductsReward() {
        PointsLog reward = new PointsLog();
        reward.setPointsDelta(199);
        when(pointsLogMapper.selectOne(any())).thenReturn(reward);
        when(pointsLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(1L)).thenReturn(user(199, 199));
        when(pointsLogMapper.insert(any(PointsLog.class))).thenReturn(1);

        service.revokeOrderPoints("MM-1", 1L);

        verify(userMapper).updateById(any(User.class));
        verify(pointsLogMapper).insert(any(PointsLog.class));
    }

    @Test
    void adjustRejectsNegativeBalance() {
        when(userMapper.selectById(1L)).thenReturn(user(10, 10));

        assertThatThrownBy(() -> service.adjust(1L, -20, "测试", 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为负");
    }

    @Test
    void memberLevelUsesTotalPoints() {
        when(userMapper.selectById(1L)).thenReturn(user(250, 600));

        var member = service.member(1L);

        assertThat(member.level()).isEqualTo("GOLD");
        assertThat(member.points()).isEqualTo(250);
        assertThat(member.nextLevelPoints()).isEqualTo(2000);
    }

    private User user(int points, int totalPoints) {
        User user = new User();
        user.setId(1L);
        user.setPoints(points);
        user.setTotalPoints(totalPoints);
        user.setMemberLevel("NORMAL");
        return user;
    }
}
