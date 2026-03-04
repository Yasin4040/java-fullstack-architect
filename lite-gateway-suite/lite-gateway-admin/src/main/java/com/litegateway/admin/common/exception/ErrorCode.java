package com.litegateway.admin.common.exception;

/**
 * 错误码接口
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
public interface ErrorCode {
    String getCode();
    String getMessage();
}
