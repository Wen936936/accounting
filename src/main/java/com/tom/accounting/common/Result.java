package com.tom.accounting.common;

import lombok.Data;

// 统一返回结果封装，所有接口都返回该格式 {code, msg, data}
@Data
public class Result<T> {

    // 状态码，200 表示成功
    private Integer code;
    // 提示信息
    private String msg;
    // 业务数据
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功，带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功，无数据
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    // 失败，指定状态码和提示
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // 失败，默认状态码 500
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}
