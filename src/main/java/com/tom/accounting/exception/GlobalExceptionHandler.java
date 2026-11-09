package com.tom.accounting.exception;

import com.tom.accounting.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 全局异常处理器，统一捕获异常并转换为 Result 格式返回
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常，返回异常中携带的错误码和提示
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理请求路径不存在的情况，HTTP 状态码与业务 code 同时返回 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, "接口不存在"));
    }

    // 处理鉴权异常，HTTP 状态码与业务 code 同时返回 401
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<Result<Void>> handleTokenException(TokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, e.getMessage()));
    }

    // 处理请求方法不支持（如 GET 访问只支持 POST 的接口），HTTP 状态码与业务 code 同时返回 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Result.error(405, "请求方法不支持"));
    }

    // 处理请求体格式错误（非法 JSON、日期格式不匹配等），HTTP 状态码与业务 code 同时返回 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(400, "请求参数格式错误"));
    }

    // 处理 @Valid 参数校验失败，HTTP 状态码与业务 code 同时返回 400，返回第一条校验错误信息
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(400, msg));
    }

    // 兜底处理其他未知异常，不向前端暴露具体错误信息
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(500, "系统异常，请稍后重试"));
    }
}
