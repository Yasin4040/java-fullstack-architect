package com.litegateway.admin.common;

import com.litegateway.admin.common.exception.BizExceptionAssert;

/**
 * 错误码枚举
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 * 移除了公司特定的错误码
 */
public enum ErrorCodeEnum implements BizExceptionAssert {
    SUCCESS("00000", "成功"),
    USER_ERROR_A0201("A0201", "用户账户不存在"),
    USER_ERROR_A0202("A0202", "用户账户被冻结"),
    USER_ERROR_A0203("A0203", "用户账户已作废"),
    USER_ERROR_A0210("A0210", "用户密码错误"),
    USER_ERROR_A0211("A0211", "用户输入密码错误次数超限"),
    USER_ERROR_A0220("A0220", "用户身份校验失败"),
    USER_ERROR_A0230("A0230", "用户登录已过期"),
    USER_ERROR_A0231("A0231", "用户未登录"),
    USER_ERROR_A0240("A0240", "用户验证码错误"),
    USER_ERROR_A0300("A0300", "访问权限异常"),
    USER_ERROR_A0301("A0301", "访问未授权"),
    USER_ERROR_A0400("A0400", "用户请求参数错误"),
    USER_ERROR_A0404("A0404", "找不到路径"),
    USER_ERROR_A0410("A0410", "请求必填参数为空"),
    USER_ERROR_A0421("A0421", "参数格式不匹配"),
    USER_ERROR_A0500("A0500", "用户请求服务异常"),
    USER_ERROR_A0501("A0501", "请求次数超出限制"),
    SYSTEM_ERROR_B0001("B0001", "系统执行出错"),
    SYSTEM_ERROR_B0100("B0100", "系统执行超时"),
    SYSTEM_ERROR_B0210("B0210", "系统限流"),
    SYSTEM_ERROR_B0300("B0300", "系统资源异常"),
    SERVICE_ERROR_C0001("C0001", "调用第三方服务出错"),
    SERVICE_ERROR_C0100("C0100", "中间件服务出错"),
    SERVICE_ERROR_C0130("C0130", "缓存服务出错"),
    SERVICE_ERROR_C0300("C0300", "数据库服务出错"),
    SERVICE_ERROR_C0311("C0311", "表不存在"),
    SERVICE_ERROR_C0341("C0341", "主键冲突"),
    GATEWAY_ERROR_G0001("G0001", "网关路由不存在"),
    GATEWAY_ERROR_G0002("G0002", "网关路由已存在"),
    GATEWAY_ERROR_G0003("G0003", "Nacos服务查询失败"),
    GATEWAY_ERROR_G0004("G0004", "路由配置解析失败");

    private final String code;
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
