package com.litegateway.core.common.web;

import lombok.Data;

/**
 * 统一响应结果封装
 * 与 Admin 模块保持一致
 */
@Data
public class Result<T> {

    private String code;
    private String message;
    private T data;

    public boolean isOk() {
        //先写200个把
        return "00000".equals(this.code)|| "200".equals(this.code);
    }
}
