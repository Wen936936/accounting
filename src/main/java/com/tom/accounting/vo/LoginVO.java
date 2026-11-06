package com.tom.accounting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 登录成功返回的数据
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    // JWT 令牌，前端后续请求需放在 Authorization 头中
    private String token;

    // 用户昵称
    private String nickname;
}
