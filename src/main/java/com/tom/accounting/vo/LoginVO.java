package com.tom.accounting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 登录成功返回的数据
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    // 访问令牌，有效期 30 分钟，前端后续请求需放在 Authorization 头中
    private String accessToken;

    // 刷新令牌，有效期 7 天，仅用于调用刷新接口换取新的 AccessToken
    private String refreshToken;

    // 用户昵称
    private String nickname;
}
