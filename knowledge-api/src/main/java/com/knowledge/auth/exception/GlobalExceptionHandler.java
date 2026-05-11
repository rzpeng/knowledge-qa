package com.knowledge.auth.exception;

import com.knowledge.auth.dto.R;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(403, "权限不足");
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public R<Void> handleBadCredentials() {
        return R.fail(401, "用户名或密码错误");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        return R.fail(500, e.getMessage() != null ? e.getMessage() : "服务器内部错误");
    }
}
