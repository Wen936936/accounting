package com.tom.accounting.service;

import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.vo.AccessTokenVO;
import com.tom.accounting.vo.LoginVO;

// 用户业务接口
public interface UserService {

    // 用户注册，用户名重复时抛出 BusinessException
    void register(RegisterDTO registerDTO);

    // 用户登录，成功返回双 Token 和昵称，失败抛出 BusinessException
    LoginVO login(LoginDTO loginDTO);

    // 刷新 AccessToken，RefreshToken 非法、过期或与 Redis 不匹配时抛出 BusinessException
    AccessTokenVO refreshAccessToken(String refreshToken);

    // 用户登出，删除 Redis 中保存的 RefreshToken
    void logout(Long userId);
}
