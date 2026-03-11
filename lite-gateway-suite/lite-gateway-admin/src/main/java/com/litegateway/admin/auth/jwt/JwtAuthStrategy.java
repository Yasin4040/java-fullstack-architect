package com.litegateway.admin.auth.jwt;

import com.litegateway.admin.auth.*;
import com.litegateway.admin.auth.AuthProperties.JwtConfig;
import com.litegateway.admin.repository.entity.SysUser;
import com.litegateway.admin.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

/**
 * JWT 内置认证策略
 * 默认启用，无需外部依赖
 *
 * 适用场景：中小团队快速启动，单点部署
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "lite.gateway.auth.type",
        havingValue = "jwt",
        matchIfMissing = true  // 默认使用 JWT
)
public class JwtAuthStrategy implements AuthStrategy {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private SysUserService userService;

    @Autowired
    private AuthProperties authProperties;

    @Override
    public AuthenticationResult authenticate(LoginRequest request) {
        log.info("JWT authentication for user: {}", request.getUsername());

        try {
            // 1. 验证用户名密码
            SysUser user = userService.validateCredentials(
                    request.getUsername(),
                    request.getPassword()
            );

            if (user == null) {
                return AuthenticationResult.failure("用户名或密码错误");
            }

            // 2. 检查用户状态
            if (user.getStatus() != 0) {
                return AuthenticationResult.failure("用户已被禁用");
            }

            // 3. 生成双 Token
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            // 4. 构建用户信息
            UserInfo userInfo = UserInfo.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .realName(user.getRealName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .roles(Collections.singletonList("ADMIN")) // 默认角色，后续可从数据库读取
                    .permissions(Collections.emptyList()) // 默认空权限列表
                    .build();

            // 5. 返回结果
            Long expiresIn = authProperties.getJwt().getAccessTokenExpiration() / 1000;
            return AuthenticationResult.success(accessToken, refreshToken, expiresIn, userInfo);

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            return AuthenticationResult.failure("认证失败：" + e.getMessage());
        }
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    @Override
    public UserDetails getUserDetails(String token) {
        Long userId = tokenProvider.extractUserId(token);
        String username = tokenProvider.extractUsername(token);
        String role = tokenProvider.extractRole(token);

        return UserDetails.builder()
                .userId(userId)
                .username(username)
                .roles(role != null ? Arrays.asList(role) : Collections.singletonList("USER"))
                .enabled(true)
                .build();
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!tokenProvider.validateToken(refreshToken)) {
            return AuthenticationResult.failure("无效的刷新令牌");
        }

        // 从 Refresh Token 中提取用户信息
        Long userId = tokenProvider.extractUserId(refreshToken);
        SysUser user = userService.getById(userId);

        if (user == null || user.getStatus() != 0) {
            return AuthenticationResult.failure("用户不存在或已被禁用");
        }

        // 生成新的 Token 对
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        UserInfo userInfo = UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .roles(Collections.singletonList("ADMIN"))
                .permissions(Collections.emptyList())
                .build();

        Long expiresIn = authProperties.getJwt().getAccessTokenExpiration() / 1000;
        return AuthenticationResult.success(newAccessToken, newRefreshToken, expiresIn, userInfo);
    }

    @Override
    public void logout(String token) {
        // JWT 无状态，服务端无需处理
        // 如需实现黑名单，可将 Token 加入 Redis 黑名单
        log.info("User logout, token: {}", token.substring(0, Math.min(10, token.length())) + "...");
    }

    @Override
    public String getAuthType() {
        return "jwt";
    }
}
