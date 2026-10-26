package com.mallmini.address.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mallmini.address.dto.AddressRequest;
import com.mallmini.address.entity.UserAddress;
import com.mallmini.address.mapper.UserAddressMapper;
import com.mallmini.address.vo.AddressVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper addressMapper;

    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public AddressVO create(Long userId, AddressRequest request) {
        long count = addressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId));
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || count == 0;
        if (makeDefault) {
            clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(request.receiverName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district().trim());
        address.setDetail(request.detail().trim());
        address.setIsDefault(makeDefault);
        addressMapper.insert(address);
        return toVO(address);
    }

    @Transactional
    public AddressVO update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearDefault(userId);
        }

        address.setReceiverName(request.receiverName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district().trim());
        address.setDetail(request.detail().trim());
        address.setIsDefault(makeDefault);
        addressMapper.updateById(address);
        return toVO(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        requireOwned(userId, addressId);
        addressMapper.deleteById(addressId);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        UserAddress address = requireOwned(userId, addressId);
        clearDefault(userId);
        address.setIsDefault(true);
        addressMapper.updateById(address);
    }

    public UserAddress requireOwned(Long userId, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, true)
                .set(UserAddress::getIsDefault, false));
    }

    private AddressVO toVO(UserAddress address) {
        return new AddressVO(
                address.getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }
}
