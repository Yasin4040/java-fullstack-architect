package com.litegateway.admin.controller;

import com.litegateway.admin.common.exception.ErrorCodeDefinition;
import com.litegateway.admin.common.web.Result;
import com.litegateway.admin.service.ErrorCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错误码配置接口
 * 提供给前端获取错误码配置
 */
@Tag(name = "错误码配置", description = "错误码配置管理")
@RestController
@RequestMapping("/config")
public class ErrorCodeController {
    
    @Autowired
    private ErrorCodeService errorCodeService;
    
    /**
     * 获取所有错误码配置
     */
    @Operation(summary = "获取错误码配置", description = "获取所有错误码配置，供前端使用")
    @GetMapping("/error-codes")
    public Result<List<ErrorCodeDefinition>> getErrorCodes() {
        return Result.ok(errorCodeService.getAllErrorCodes());
    }
}
