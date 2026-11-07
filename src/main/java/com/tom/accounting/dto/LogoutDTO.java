package com.tom.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 登出请求参数
@Data
public class LogoutDTO {

    // RefreshToken，必填，用于解析出当前用户 id
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    // 设备类型，需与登录时一致，默认 web
    @NotBlank(message = "deviceType 不能为空")
    @Size(max = 20, message = "deviceType 长度不能超过 20")
    private String deviceType = "web";
}
