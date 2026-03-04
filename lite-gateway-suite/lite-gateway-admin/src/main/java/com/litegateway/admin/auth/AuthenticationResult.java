package com.litegateway.admin.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResult {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** Token 类型：Bearer */
    private String tokenType;

    /** Access Token 有效期（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserInfo userInfo;

    /** 是否成功 */
    private boolean success;

    /** 错误信息 */
    private String errorMessage;

    public static AuthenticationResult success(String accessToken, String refreshToken, Long expiresIn, UserInfo userInfo) {
        return AuthenticationResult.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userInfo(userInfo)
                .build();
    }

    public static AuthenticationResult failure(String errorMessage) {
        return AuthenticationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
