package com.tom.accounting.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

// JWT 工具类，负责生成和解析 AccessToken、RefreshToken
@Component
public class JwtUtil {

    // 令牌类型：访问令牌，用于访问业务接口
    public static final String TYPE_ACCESS = "access";
    // 令牌类型：刷新令牌，仅用于换取新的 AccessToken
    public static final String TYPE_REFRESH = "refresh";

    // claims 中存放用户 id 的键名
    private static final String CLAIM_USER_ID = "userId";
    // claims 中存放令牌类型的键名
    private static final String CLAIM_TYPE = "type";

    // HS256 签名密钥，由 application.yml 中的 jwt.secret 生成
    private final SecretKey secretKey;
    // AccessToken 有效期（毫秒）
    private final long accessExpirationMs;
    // RefreshToken 有效期（毫秒）
    private final long refreshExpirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
                   @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // 生成 AccessToken，type 为 access
    public String generateAccessToken(Long userId) {
        return generateToken(userId, TYPE_ACCESS, accessExpirationMs);
    }

    // 生成 RefreshToken，type 为 refresh
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, TYPE_REFRESH, refreshExpirationMs);
    }

    // 获取 AccessToken 有效期，用于计算返回给前端的 expiresIn
    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    // 获取 RefreshToken 有效期，用于设置 Redis 过期时间
    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    // 解析令牌并取出用户 id，签名错误、已过期、类型不匹配或缺少用户 id 时抛出 JwtException
    public Long parseUserId(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(CLAIM_TYPE))) {
            throw new JwtException("令牌类型不正确");
        }
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId == null) {
            throw new JwtException("令牌缺少用户信息");
        }
        // JSON 数字可能被解析为 Integer 或 Long，统一转为 Long
        return ((Number) userId).longValue();
    }

    // 生成令牌，写入用户 id、令牌类型、签发时间和过期时间
    private String generateToken(Long userId, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
