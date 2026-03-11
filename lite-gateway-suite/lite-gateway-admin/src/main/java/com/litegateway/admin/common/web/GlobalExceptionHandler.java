package com.litegateway.admin.common.web;

import com.litegateway.admin.common.ErrorCodeEnum;
import com.litegateway.admin.common.exception.BizException;
import com.litegateway.admin.common.exception.ErrorCodeDefinition;
import com.litegateway.admin.service.ErrorCodeService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Autowired
    private ErrorCodeService errorCodeService;
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        String code = e.getCode();
        String message = e.getMsg();
        
        // 获取错误码配置
        ErrorCodeDefinition config = errorCodeService.getErrorCode(code);
        
        // 根据配置决定是否记录日志
        if (config != null && Boolean.TRUE.equals(config.getLogStackTrace())) {
            log.error("Business error [{}] at {}: {}", code, request.getRequestURI(), message, e);
        } else {
            log.warn("Business error [{}] at {}: {}", code, request.getRequestURI(), message);
        }
        
        return Result.failure(code, message);
    }
    
    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error at {}: {}", request.getRequestURI(), message);
        
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0400.getCode(), 
                "参数错误: " + message);
    }
    
    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Bind error at {}: {}", request.getRequestURI(), message);
        
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0400.getCode(), 
                "参数错误: " + message);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), e.getMessage());
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0400.getCode(), e.getMessage());
    }
    
    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("Illegal state at {}: {}", request.getRequestURI(), e.getMessage());
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0500.getCode(), e.getMessage());
    }
    
    /**
     * 处理JWT过期异常
     */
    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleExpiredJwtException(ExpiredJwtException e, HttpServletRequest request) {
        log.warn("JWT expired at {}: {}", request.getRequestURI(), e.getMessage());
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0230.getCode(), 
                "用户登录已过期，请重新登录");
    }
    
    /**
     * 处理其他JWT异常
     */
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT error at {}: {}", request.getRequestURI(), e.getMessage());
        return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                "认证令牌无效，请重新登录");
    }
    
    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("System error at {}: {}", request.getRequestURI(), e.getMessage(), e);
        
        // 返回通用错误，不暴露内部细节
        return Result.failure(ErrorCodeEnum.SYSTEM_ERROR_B0001.getCode(), 
                "系统繁忙，请稍后重试");
    }
}
