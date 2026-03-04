package com.litegateway.admin.service;

import com.litegateway.admin.dto.GatewayConfigDTO;

/**
 * 配置服务接口
 */
public interface ConfigService {

    /**
     * 获取完整网关配置
     */
    GatewayConfigDTO getGatewayConfig();

    /**
     * 获取配置版本号
     */
    Long getConfigVersion();

    /**
     * 增加配置版本号（配置变更时调用）
     */
    void incrementVersion();
}
