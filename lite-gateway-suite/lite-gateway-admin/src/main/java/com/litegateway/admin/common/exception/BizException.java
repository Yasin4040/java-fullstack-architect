package com.litegateway.admin.common.exception;

import com.litegateway.admin.common.ErrorCodeEnum;

/**
 * 业务异常
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
public class BizException extends RuntimeException {

    /**
     * 业务状态码
     */
    private String code;
    /**
     * 业务信息
     */
    private String message;

    /**
     * 根异常，保持异常链
     */
    private Throwable caused;

    public BizException(ErrorCodeEnum resultEnum, Throwable caused) {
        super(caused);
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(ErrorCode code) {
        super(code.getMessage());
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public BizException(String code, Throwable caused) {
        super(caused);
        this.code = code;
        this.caused = caused;
    }

    public BizException(String code, String message, Throwable caused) {
        super(message, caused);
        this.code = code;
        this.message = message;
        this.caused = caused;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return String.format("异常code=%s, 原因=%s", code, message);
    }

    public Throwable getCaused() {
        return caused;
    }

    public String getMsg() {
        return message;
    }
}
