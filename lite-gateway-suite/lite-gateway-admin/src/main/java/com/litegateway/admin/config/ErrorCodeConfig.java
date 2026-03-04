package com.litegateway.admin.config;

import com.litegateway.admin.common.exception.ErrorCodeDefinition;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 错误码配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "lite.gateway.error")
public class ErrorCodeConfig {
    
    /**
     * 错误码列表
     */
    private List<ErrorCodeDefinition> codes = new ArrayList<>();
}
