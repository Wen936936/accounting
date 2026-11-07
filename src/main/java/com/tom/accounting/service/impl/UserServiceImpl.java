package com.tom.accounting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tom.accounting.dto.LoginDTO;
import com.tom.accounting.dto.LogoutDTO;
import com.tom.accounting.dto.RefreshDTO;
import com.tom.accounting.dto.RegisterDTO;
import com.tom.accounting.entity.User;
import com.tom.accounting.exception.BusinessException;
import com.tom.accounting.exception.TokenException;
import com.tom.accounting.mapper.UserMapper;
import com.tom.accounting.service.UserService;
import com.tom.accounting.util.JwtUtil;
import com.tom.accounting.vo.LoginVO;
import com.tom.accounting.vo.TokenVO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

// 用户业务实现类
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Redis 中保存 RefreshToken 的键前缀，完整键为 refresh_token:{userId}:{deviceType}
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    // Redis 中记录登录失败次数的键前缀，完整键为 login_fail:{username}
    private static final String LOGIN_FAIL_KEY_PREFIX = "login_fail:";
    private static final long MAX_LOGIN_FAIL_COUNT = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final String ACCOUNT_LOCKED_MSG = "账号已锁定，请15分钟后再试";
    // 原子轮换 RefreshToken：仅当 Redis 中的值等于旧令牌时，才覆盖为新令牌并重置 TTL
    private static final DefaultRedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

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
        String loginFailKey = LOGIN_FAIL_KEY_PREFIX + loginDTO.getUsername();
        // 锁定期间即使密码正确也拒绝登录
        if (isLoginLocked(loginFailKey)) {
            throw new BusinessException(ACCOUNT_LOCKED_MSG);
        }

        // 按用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        // 用户不存在时不累加失败次数，防止恶意锁定他人账号
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            Long failCount = stringRedisTemplate.opsForValue().increment(loginFailKey);
            stringRedisTemplate.expire(loginFailKey, LOGIN_LOCK_DURATION);
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                throw new BusinessException(ACCOUNT_LOCKED_MSG);
            }
            throw new BusinessException("用户名或密码错误");
        }

        stringRedisTemplate.delete(loginFailKey);
        TokenVO tokens = issueTokens(user.getId(), loginDTO.getDeviceType());
        return new LoginVO(tokens.getAccessToken(), tokens.getRefreshToken(),
                tokens.getExpiresIn(), user.getNickname());
    }

    @Override
    public TokenVO refreshTokens(RefreshDTO refreshDTO) {
        Long userId = parseRefreshUserId(refreshDTO.getRefreshToken());
        // 防止已注销的用户继续通过旧令牌换取新令牌
        if (userMapper.selectById(userId) == null) {
            throw new TokenException("用户不存在，请重新登录");
        }

        String accessToken = jwtUtil.generateAccessToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        // 校验旧令牌与覆盖新令牌在同一个 Lua 脚本中原子完成，防止并发重复刷新
        Long rotated = stringRedisTemplate.execute(ROTATE_REFRESH_TOKEN_SCRIPT,
                List.of(refreshTokenKey(userId, refreshDTO.getDeviceType())),
                refreshDTO.getRefreshToken(), newRefreshToken,
                String.valueOf(jwtUtil.getRefreshExpirationMs()));
        if (!Long.valueOf(1L).equals(rotated)) {
            throw new TokenException("RefreshToken无效，请重新登录");
        }

        return new TokenVO(accessToken, newRefreshToken, jwtUtil.getAccessExpirationMs() / 1000);
    }

    @Override
    public void logout(LogoutDTO logoutDTO) {
        Long userId = parseRefreshUserId(logoutDTO.getRefreshToken());
        String key = refreshTokenKey(userId, logoutDTO.getDeviceType());
        // 仅允许删除与当前 RefreshToken 一致的记录，历史废弃令牌不能踢下线当前会话
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(key);
        if (!logoutDTO.getRefreshToken().equals(storedRefreshToken)) {
            throw new TokenException("RefreshToken无效，请重新登录");
        }
        stringRedisTemplate.delete(key);
    }

    // 签发双 Token 并写入 RefreshToken，覆盖同设备的旧记录并重置 TTL
    private TokenVO issueTokens(Long userId, String deviceType) {
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        stringRedisTemplate.opsForValue().set(refreshTokenKey(userId, deviceType), refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshExpirationMs()));
        return new TokenVO(accessToken, refreshToken, jwtUtil.getAccessExpirationMs() / 1000);
    }

    // 解析 RefreshToken 得到用户 id，过期或无效时抛出 TokenException
    private Long parseRefreshUserId(String refreshToken) {
        try {
            return jwtUtil.parseUserId(refreshToken, JwtUtil.TYPE_REFRESH);
        } catch (ExpiredJwtException e) {
            throw new TokenException("RefreshToken已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException("RefreshToken无效，请重新登录");
        }
    }

    private boolean isLoginLocked(String loginFailKey) {
        String failCount = stringRedisTemplate.opsForValue().get(loginFailKey);
        return failCount != null && Long.parseLong(failCount) >= MAX_LOGIN_FAIL_COUNT;
    }

    // 拼接 Redis 中 RefreshToken 的完整键，按用户和设备区分多端登录
    private String refreshTokenKey(Long userId, String deviceType) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceType;
    }
}
