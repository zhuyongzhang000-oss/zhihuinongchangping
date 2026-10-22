package com.agritrace.common;

import lombok.Getter;

/** 业务异常：抛出后由 GlobalExceptionHandler 转成 Result 返回给前端 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String msg) {
        this(400, msg);
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
