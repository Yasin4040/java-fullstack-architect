package com.litegateway.admin.auth;

/**
 * 认证策略接口
 * 支持多种认证方式：JWT、OAuth2、LDAP
 *
 * 切换方式：修改配置 lite.gateway.auth.type
 * 实现类通过 @ConditionalOnProperty 自动装配
 */
public interface AuthStrategy {

    /**
     * 执行登录认证
     * @param request 登录请求
     * @return 认证结果（包含 Token 或用户信息）
     */
    AuthenticationResult authenticate(LoginRequest request);

    /**
     * 验证 Token 有效性
     * @param token 访问令牌
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 从 Token 中提取用户信息
     * @param token 访问令牌
     * @return 用户详情
     */
    UserDetails getUserDetails(String token);

    /**
     * 刷新 Access Token
     * @param refreshToken 刷新令牌
     * @return 新的认证结果
     */
    AuthenticationResult refreshToken(String refreshToken);

    /**
     * 登出处理
     * @param token 当前 Token
     */
    void logout(String token);

    /**
     * 获取认证类型
     * @return 认证类型标识
     */
    String getAuthType();
}
