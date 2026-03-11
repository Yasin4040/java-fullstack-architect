package com.litegateway.admin.controller;

import com.litegateway.admin.auth.*;
import com.litegateway.admin.common.ErrorCodeEnum;
import com.litegateway.admin.common.web.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
@RequestMapping("/gateway/auth")
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
            // 根据错误消息判断具体的错误类型
            String errorMessage = result.getErrorMessage();
            if (errorMessage != null && errorMessage.contains("密码")) {
                return Result.failure(ErrorCodeEnum.USER_ERROR_A0210.getCode(), errorMessage);
            } else if (errorMessage != null && errorMessage.contains("禁用")) {
                return Result.failure(ErrorCodeEnum.USER_ERROR_A0202.getCode(), errorMessage);
            } else {
                return Result.failure(ErrorCodeEnum.USER_ERROR_A0220.getCode(), errorMessage);
            }
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
            // Refresh token 失败，返回登录过期错误码
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0230.getCode(), 
                    result.getErrorMessage());
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

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据 Token 获取当前登录用户信息")
    public Result<UserInfo> getUserInfo(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "未提供认证令牌");
        }

        try {
            UserDetails userDetails = authStrategy.getUserDetails(token);
            // 转换为 UserInfo
            UserInfo userInfo = UserInfo.builder()
                    .userId(userDetails.getUserId())
                    .username(userDetails.getUsername())
                    .roles(userDetails.getRoles())
                    .build();

            return Result.ok(userInfo);
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired when getting user info: {}", e.getMessage());
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0230.getCode(), 
                    "用户登录已过期，请重新登录");
        } catch (JwtException e) {
            log.warn("Invalid JWT when getting user info: {}", e.getMessage());
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "认证令牌无效，请重新登录");
        } catch (Exception e) {
            log.error("Failed to get user info: {}", e.getMessage(), e);
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "获取用户信息失败");
        }
    }

    @GetMapping("/codes")
    @Operation(summary = "获取用户权限码", description = "获取当前用户的权限码列表")
    public Result<String[]> getAccessCodes(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "未提供认证令牌");
        }

        try {
            UserDetails userDetails = authStrategy.getUserDetails(token);
            // 假设 roles 就是权限码
            return Result.ok(userDetails.getRoles().toArray(new String[0]));
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired when getting access codes: {}", e.getMessage());
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0230.getCode(), 
                    "用户登录已过期，请重新登录");
        } catch (JwtException e) {
            log.warn("Invalid JWT when getting access codes: {}", e.getMessage());
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "认证令牌无效，请重新登录");
        } catch (Exception e) {
            log.error("Failed to get access codes: {}", e.getMessage(), e);
            return Result.failure(ErrorCodeEnum.USER_ERROR_A0231.getCode(), 
                    "获取权限码失败");
        }
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
