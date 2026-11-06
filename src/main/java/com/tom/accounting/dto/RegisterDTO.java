package com.tom.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 注册请求参数
@Data
public class RegisterDTO {

    // 用户名，必填，最长 50 字符
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50 个字符")
    private String username;

    // 密码，必填，6 到 32 字符
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 个字符之间")
    private String password;

    // 昵称，可选，最长 50 字符
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
}
