package com.mallmini.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 64, message = "收货人过长")
        String receiverName,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "省份不能为空")
        @Size(max = 64, message = "省份过长")
        String province,

        @NotBlank(message = "城市不能为空")
        @Size(max = 64, message = "城市过长")
        String city,

        @NotBlank(message = "区县不能为空")
        @Size(max = 64, message = "区县过长")
        String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址过长")
        String detail,

        Boolean isDefault
) {
}
