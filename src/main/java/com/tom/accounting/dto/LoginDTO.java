package com.tom.accounting.dto;

import jakarta.validation.constraints.NotBlank;
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
}
