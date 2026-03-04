package com.litegateway.admin.controller;

import com.litegateway.admin.auth.*;
import com.litegateway.admin.common.exception.ErrorCode;
import com.litegateway.admin.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 统一入口，底层根据配置自动路由到不同策略
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "登录、登出、Token 刷新（支持 JWT/OAuth2/LDAP 三种模式）")
public class AuthController {

    @Autowired
    private AuthStrategy authStrategy;

    @Autowired
    private AuthProperties authProperties;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持 JWT、LDAP 模式（OAuth2 请使用授权码流程）")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("Login attempt: {}", request.getUsername());

        AuthenticationResult result = authStrategy.authenticate(request);

        if (!result.isSuccess()) {
            return Result.failure(new ErrorCode() {
                @Override
                public String getCode() { return "401"; }
                @Override
                public String getMessage() { return result.getErrorMessage(); }
            });
        }

        LoginResponse response = LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType(result.getTokenType())
                .expiresIn(result.getExpiresIn())
                .userInfo(result.getUserInfo())
                .build();

        return Result.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 换取新的 Access Token")
    public Result<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        log.info("Token refresh attempt");

        AuthenticationResult result = authStrategy.refreshToken(request.getRefreshToken());

        if (!result.isSuccess()) {
            return Result.failure(new ErrorCode() {
                @Override
                public String getCode() { return "401"; }
                @Override
                public String getMessage() { return result.getErrorMessage(); }
            });
        }

        LoginResponse response = LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType(result.getTokenType())
                .expiresIn(result.getExpiresIn())
                .userInfo(result.getUserInfo())
                .build();

        return Result.ok(response);
    }

    @GetMapping("/user-info")
    @Operation(summary = "获取当前用户信息", description = "根据 Token 获取当前登录用户信息")
    public Result<UserInfo> getUserInfo(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.failure(new ErrorCode() {
                @Override
                public String getCode() { return "401"; }
                @Override
                public String getMessage() { return "未提供认证令牌"; }
            });
        }

        UserDetails userDetails = authStrategy.getUserDetails(token);
        // 转换为 UserInfo
        UserInfo userInfo = UserInfo.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .roles(userDetails.getRoles())
                .build();

        return Result.ok(userInfo);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登出当前用户，作废 Token")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authStrategy.logout(token);
        }
        return Result.ok();
    }

    /**
     * 获取当前认证配置信息
     * 帮助前端适配不同认证模式
     */
    @GetMapping("/config")
    @Operation(summary = "获取认证配置", description = "返回当前认证类型和配置，供前端适配")
    public Result<AuthConfigResponse> getAuthConfig() {
        AuthConfigResponse config = new AuthConfigResponse();
        config.setAuthType(authProperties.getType());
        config.setOauth2Enabled("oauth2".equals(authProperties.getType()));
        config.setLdapEnabled("ldap".equals(authProperties.getType()));

        // OAuth2 模式下，返回授权端点
        if ("oauth2".equals(authProperties.getType())) {
            config.setOauth2AuthorizationUri(buildAuthorizationUri());
        }

        return Result.ok(config);
    }

    /**
     * 从请求中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 构建 OAuth2 授权 URI
     */
    private String buildAuthorizationUri() {
        AuthProperties.OAuth2Config oauth2 = authProperties.getOauth2();
        return String.format(
                "%s/protocol/openid-connect/auth?client_id=%s&response_type=code&redirect_uri=%s",
                oauth2.getIssuerUri(),
                oauth2.getClientId(),
                "http://localhost:8081/oauth2/callback" // 应该配置化
        );
    }
}
