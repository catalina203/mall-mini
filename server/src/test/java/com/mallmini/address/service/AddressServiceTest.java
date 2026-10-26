package com.mallmini.address.service;

import com.mallmini.address.dto.AddressRequest;
import com.mallmini.address.entity.UserAddress;
import com.mallmini.address.mapper.UserAddressMapper;
import com.mallmini.address.vo.AddressVO;
import com.mallmini.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private UserAddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserAddress.class);
    }

    @Test
    void firstAddressBecomesDefault() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(1L);
            return 1;
        });

        AddressVO result = addressService.create(1L, request(false));

        assertThat(result.isDefault()).isTrue();
        verify(addressMapper).update(isNull(), any());
    }

    @Test
    void createDefaultClearsOldDefault() {
        when(addressMapper.selectCount(any())).thenReturn(2L);
        when(addressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(5L);
            return 1;
        });

        AddressVO result = addressService.create(1L, request(true));

        assertThat(result.isDefault()).isTrue();
        verify(addressMapper).update(isNull(), any());
    }

    @Test
    void updateRejectsOtherUsersAddress() {
        UserAddress address = address(7L, 2L, true);
        when(addressMapper.selectById(7L)).thenReturn(address);

        assertThatThrownBy(() -> addressService.update(1L, 7L, request(false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("地址不存在");
    }

    @Test
    void deleteRejectsOtherUsersAddress() {
        when(addressMapper.selectById(7L)).thenReturn(address(7L, 2L, false));

        assertThatThrownBy(() -> addressService.delete(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("地址不存在");
    }

    @Test
    void setDefaultClearsOthersAndUpdatesAddress() {
        UserAddress address = address(3L, 1L, false);
        when(addressMapper.selectById(3L)).thenReturn(address);

        addressService.setDefault(1L, 3L);

        assertThat(address.getIsDefault()).isTrue();
        verify(addressMapper).update(isNull(), any());
        verify(addressMapper).updateById(address);
    }

    private AddressRequest request(boolean isDefault) {
        return new AddressRequest("张三", "13800138000", "广东省", "深圳市", "南山区", "科技园路 1 号", isDefault);
    }

    private UserAddress address(Long id, Long userId, boolean isDefault) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园路 1 号");
        address.setIsDefault(isDefault);
        return address;
    }
}
