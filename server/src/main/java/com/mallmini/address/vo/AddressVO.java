package com.mallmini.address.vo;

public record AddressVO(
        Long id,
        String receiverName,
        String phone,
        String province,
        String city,
        String district,
        String detail,
        boolean isDefault
) {
}
