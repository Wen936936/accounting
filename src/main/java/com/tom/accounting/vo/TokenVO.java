package com.tom.accounting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 刷新令牌成功返回的数据，RefreshToken 已轮换，旧的立即失效
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    // 新签发的 AccessToken
    private String accessToken;

    // 新签发的 RefreshToken，需替换本地保存的旧值
    private String refreshToken;

    // AccessToken 有效期（秒）
    private Long expiresIn;
}
