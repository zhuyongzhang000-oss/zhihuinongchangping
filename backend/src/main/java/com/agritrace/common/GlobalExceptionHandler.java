package com.agritrace.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> business(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> bind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getDefaultMessage()).orElse("参数错误");
        return Result.fail(400, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public Result<Void> badRequest(Exception e) {
        return Result.fail(400, "参数格式错误");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> duplicate(DuplicateKeyException e) {
        return Result.fail(400, "编号已存在，请换一个");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> notFound(NoResourceFoundException e) {
        return Result.fail(404, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("未处理的异常", e);
        return Result.fail(500, "服务器开小差了，请稍后再试");
    }
}
