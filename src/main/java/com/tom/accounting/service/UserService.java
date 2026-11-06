package com.tom.accounting.service;

import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.vo.LoginVO;

// 用户业务接口
public interface UserService {

    // 用户注册，用户名重复时抛出 BusinessException
    void register(RegisterDTO registerDTO);

    // 用户登录，成功返回 JWT 令牌和昵称，失败抛出 BusinessException
    LoginVO login(LoginDTO loginDTO);
}
