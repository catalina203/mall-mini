package com.mallmini.order.vo;

public record AddressSnapshotVO(
        String receiverName,
        String phone,
        String province,
        String city,
        String district,
        String detail
) {
}
