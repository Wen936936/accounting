package com.tom.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 刷新令牌请求参数
@Data
public class RefreshDTO {

    // RefreshToken，必填
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    // 设备类型，需与登录时一致，默认 web
    @NotBlank(message = "deviceType 不能为空")
    @Size(max = 20, message = "deviceType 长度不能超过 20")
    private String deviceType = "web";
}
