package com.litegateway.admin.service;

import com.litegateway.admin.common.exception.ErrorCodeDefinition;

import java.util.List;
import java.util.Map;

/**
 * 错误码服务接口
 */
public interface ErrorCodeService {

    /**
     * 获取所有错误码配置
     */
    List<ErrorCodeDefinition> getAllErrorCodes();

    /**
     * 获取错误码映射（用于快速查找）
     */
    Map<String, ErrorCodeDefinition> getErrorCodeMap();

    /**
     * 根据错误码获取配置
     */
    ErrorCodeDefinition getErrorCode(String code);

    /**
     * 获取成功码配置
     */
    ErrorCodeDefinition getSuccessCode();

    /**
     * 刷新配置（重新加载）
     */
    void refresh();
}
