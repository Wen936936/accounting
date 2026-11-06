package com.tom.accounting.controller;

import com.tom.accounting.common.Result;
import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.service.UserService;
import com.tom.accounting.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 认证接口控制器，路径前缀 /api/v1/auth，该路径不经过 JWT 拦截器
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

    // 登录接口，返回 JWT 令牌
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }
}
