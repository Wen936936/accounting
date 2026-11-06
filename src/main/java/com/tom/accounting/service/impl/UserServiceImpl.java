package com.tom.accounting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.entity.User;
import com.tom.accounting.exception.BusinessException;
import com.tom.accounting.mapper.UserMapper;
import com.tom.accounting.service.UserService;
import com.tom.accounting.util.JwtUtil;
import com.tom.accounting.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 用户业务实现类
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterDTO registerDTO) {
        // 检查用户名是否已被占用
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        // 密码必须加密后再入库，明文不落库
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 按用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        // 用户不存在和密码错误使用同一提示，避免泄露账号是否存在
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 校验通过，签发 JWT，claims 中存放用户 id
        String token = jwtUtil.generateToken(user.getId());
        return new LoginVO(token, user.getNickname());
    }
}
