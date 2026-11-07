package com.tom.accounting.controller;

import com.tom.accounting.common.Result;
import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.LogoutDTO;
import com.tom.accounting.dto.RefreshDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.service.UserService;
import com.tom.accounting.vo.LoginVO;
import com.tom.accounting.vo.TokenVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 认证接口控制器，路径前缀 /api/v1/auth，注册、登录、刷新、登出接口放行拦截器
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 注册接口
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    // 登录接口，返回 AccessToken 和 RefreshToken
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    // 刷新接口，AccessToken 过期后用 RefreshToken 换取新的双 Token
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshDTO refreshDTO) {
        return Result.success(userService.refreshTokens(refreshDTO));
    }

    // 登出接口，凭 RefreshToken 删除当前设备的登录状态
    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody LogoutDTO logoutDTO) {
        userService.logout(logoutDTO);
        return Result.success();
    }
}
