package com.tom.accounting.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 工具类，负责生成和解析令牌
@Component
public class JwtUtil {

    // claims 中存放用户 id 的键名
    private static final String CLAIM_USER_ID = "userId";

    // HS256 签名密钥，由 application.yml 中的 jwt.secret 生成
    private final SecretKey secretKey;
    // 令牌有效期（毫秒），由 jwt.expiration-ms 配置
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // 生成令牌，写入用户 id、签发时间和过期时间
    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // 解析令牌并取出用户 id，签名错误或已过期时抛出 JwtException
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // JSON 数字可能被解析为 Integer 或 Long，统一转为 Long
        return ((Number) claims.get(CLAIM_USER_ID)).longValue();
    }
}
