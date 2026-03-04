package com.litegateway.admin.common.exception;

import java.text.MessageFormat;

/**
 * 业务异常断言接口
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
public interface BizExceptionAssert extends ErrorCode {
    default BizException newException() {
        return new BizException(this);
    }

    default BizException newException(Object... args) {
        String msg = MessageFormat.format(this.getMessage(), args);
        return new BizException(this.getCode(), msg);
    }

    default BizException newException(String message) {
        return new BizException(this.getCode(), message);
    }

    default BizException newException(String message, Object... args) {
        String msg = MessageFormat.format(message, args);
        return new BizException(this.getCode(), msg);
    }

    default BizException newException(Throwable t, Object... args) {
        String msg = MessageFormat.format(this.getMessage(), args);
        return new BizException(this.getCode(), msg, t);
    }
}
