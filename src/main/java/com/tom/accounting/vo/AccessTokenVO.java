package com.tom.accounting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 刷新令牌成功返回的数据
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenVO {

    // 新签发的 AccessToken
    private String accessToken;
}
