package com.tom.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 登录请求参数
@Data
public class LoginDTO {

    // 用户名，必填
    @NotBlank(message = "用户名不能为空")
    private String username;

    // 密码明文，必填，服务端用 BCrypt 比对
    @NotBlank(message = "密码不能为空")
    private String password;

    // 设备类型，如 web、app、pc，用于区分多端登录，默认 web
    @NotBlank(message = "deviceType 不能为空")
    @Size(max = 20, message = "deviceType 长度不能超过 20")
    private String deviceType = "web";
}
