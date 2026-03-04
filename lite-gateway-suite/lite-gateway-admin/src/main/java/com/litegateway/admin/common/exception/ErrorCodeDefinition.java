package com.litegateway.admin.common.exception;

import lombok.Data;

/**
 * 错误码定义
 * 用于前后端统一的错误码配置
 */
@Data
public class ErrorCodeDefinition {
    
    /**
     * 错误码
     */
    private String code;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 错误级别: info/warning/error/fatal
     */
    private String level = "error";
    
    /**
     * 前端动作: none/logout/redirect/retry
     */
    private String action = "none";
    
    /**
     * 重定向URL（当action为redirect时使用）
     */
    private String redirectUrl;
    
    /**
     * 是否显示通知
     */
    private Boolean showNotification = true;
    
    /**
     * 通知持续时间（秒）
     */
    private Integer duration = 3;
    
    /**
     * 是否记录堆栈
     */
    private Boolean logStackTrace = false;
    
    /**
     * 是否为成功码
     * 用于标识成功的响应码，如 00000
     */
    private Boolean success = false;
}
