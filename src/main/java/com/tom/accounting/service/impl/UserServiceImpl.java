package com.tom.accounting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.entity.User;
import com.tom.accounting.exception.BusinessException;
import com.tom.accounting.mapper.UserMapper;
import com.tom.accounting.service.UserService;
import com.tom.accounting.util.JwtUtil;
import com.tom.accounting.vo.AccessTokenVO;
import com.tom.accounting.vo.LoginVO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

// 用户业务实现类
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Redis 中保存 RefreshToken 的键前缀，完整键为 refresh_token:{userId}
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

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

        // 校验通过，签发双 Token，RefreshToken 存入 Redis 并设置与其有效期一致的过期时间
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        stringRedisTemplate.opsForValue().set(refreshTokenKey(user.getId()), refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshExpirationMs()));
        return new LoginVO(accessToken, refreshToken, user.getNickname());
    }

    @Override
    public AccessTokenVO refreshAccessToken(String refreshToken) {
        Long userId;
        try {
            userId = jwtUtil.parseUserId(refreshToken, JwtUtil.TYPE_REFRESH);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(401, "RefreshToken已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(401, "RefreshToken无效，请重新登录");
        }

        // 传入的令牌必须与 Redis 中保存的一致，登出后或被新登录覆盖的旧令牌都会失效
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(refreshTokenKey(userId));
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new BusinessException(401, "RefreshToken无效，请重新登录");
        }

        return new AccessTokenVO(jwtUtil.generateAccessToken(userId));
    }

    @Override
    public void logout(Long userId) {
        stringRedisTemplate.delete(refreshTokenKey(userId));
    }

    // 拼接 Redis 中 RefreshToken 的完整键
    private String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
