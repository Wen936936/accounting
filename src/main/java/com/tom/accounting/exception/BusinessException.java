package com.tom.accounting.exception;

import lombok.Getter;

// 业务异常，业务校验失败时抛出，由全局异常处理器统一返回
@Getter
public class BusinessException extends RuntimeException {

    // 业务错误码
    private final Integer code;

    // 默认错误码 400
    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
