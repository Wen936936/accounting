package com.tom.accounting.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tom.accounting.common.Result;
import com.tom.accounting.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// JWT 鉴权拦截器，校验 Authorization: Bearer <token>
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    // 通过 request.setAttribute 存入的用户 id 键名，供 Controller 获取当前用户
    public static final String USER_ID_ATTRIBUTE = "userId";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_MSG = "未登录或登录已失效";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // 请求到达 Controller 前执行，返回 true 放行，false 中断
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        // 请求头缺失或不是 Bearer 格式，直接拒绝
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return reject(response);
        }
        try {
            // 去掉 "Bearer " 前缀后解析令牌
            Long userId = jwtUtil.parseUserId(authorization.substring(BEARER_PREFIX.length()));
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌签名错误、已过期或格式非法
            return reject(response);
        }
    }

    // 返回 HTTP 401 和统一格式的错误 JSON
    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(401, UNAUTHORIZED_MSG));
        return false;
    }
}
