package com.tom.accounting.service;

import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.LogoutDTO;
import com.tom.accounting.dto.RefreshDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.vo.LoginVO;
import com.tom.accounting.vo.TokenVO;

// 用户业务接口
public interface UserService {

    // 用户注册，用户名重复时抛出 BusinessException
    void register(RegisterDTO registerDTO);

    // 用户登录，成功返回双 Token 和昵称，失败或账号锁定时抛出 BusinessException
    LoginVO login(LoginDTO loginDTO);

    // 刷新令牌，轮换 RefreshToken 并返回新的双 Token，令牌非法或不匹配时抛出 TokenException
    TokenVO refreshTokens(RefreshDTO refreshDTO);

    // 用户登出，删除当前设备在 Redis 中保存的 RefreshToken，令牌非法时抛出 TokenException
    void logout(LogoutDTO logoutDTO);
}
