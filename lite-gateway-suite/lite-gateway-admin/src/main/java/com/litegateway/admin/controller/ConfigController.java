package com.litegateway.admin.controller;

import com.litegateway.admin.common.web.Result;
import com.litegateway.admin.dto.GatewayConfigDTO;
import com.litegateway.admin.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置查询控制器
 * 为 lite-gateway-core 提供配置数据查询接口
 */
@Slf4j
@RestController
@RequestMapping("/gateway/config")
@Tag(name = "配置管理", description = "网关配置查询接口，供 Core 模块调用")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * 获取完整网关配置
     * 供 Core 模块启动时调用
     */
    @GetMapping("/gateway")
    @Operation(summary = "获取网关配置", description = "获取所有启用的路由、IP黑名单、白名单配置")
    public Result<GatewayConfigDTO> getGatewayConfig() {
        GatewayConfigDTO config = configService.getGatewayConfig();
        return Result.ok(config);
    }

    /**
     * 获取配置版本号
     * 供 Core 模块轮询检查配置是否更新
     */
    @GetMapping("/version")
    @Operation(summary = "获取配置版本", description = "获取当前配置版本号，用于检查是否需要更新")
    public Result<Long> getConfigVersion() {
        Long version = configService.getConfigVersion();
        return Result.ok(version);
    }

    /**
     * 检查配置是否有更新
     * @param clientVersion 客户端当前版本号
     */
    @GetMapping("/check")
    @Operation(summary = "检查配置更新", description = "检查配置是否有更新，有则返回最新配置")
    public Result<GatewayConfigDTO> checkConfigUpdate(@RequestParam Long clientVersion) {
        Long serverVersion = configService.getConfigVersion();
        
        if (serverVersion.equals(clientVersion)) {
            // 配置未变化，返回空
            return Result.ok(null);
        }
        
        // 配置有更新，返回最新配置
        GatewayConfigDTO config = configService.getGatewayConfig();
        return Result.ok(config);
    }
}
