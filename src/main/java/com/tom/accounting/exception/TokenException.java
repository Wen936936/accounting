package com.tom.accounting.exception;

// 鉴权异常，令牌过期、无效或刷新失败时抛出，由全局异常处理器返回 HTTP 401
public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }
}
