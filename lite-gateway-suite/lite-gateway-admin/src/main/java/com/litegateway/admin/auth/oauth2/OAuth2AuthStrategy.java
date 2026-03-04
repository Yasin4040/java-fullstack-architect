package com.litegateway.admin.auth.oauth2;

import com.litegateway.admin.auth.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * OAuth2 集成认证策略
 * 需要配置外部 OAuth2 授权服务器
 *
 * 适用场景：已有统一认证中心的企业
 * 支持：Spring Security OAuth2、Keycloak、Authing、Okta 等
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "lite.gateway.auth.type",
        havingValue = "oauth2"
)
public class OAuth2AuthStrategy implements AuthStrategy {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public AuthenticationResult authenticate(LoginRequest request) {
        // OAuth2 模式下，前端应该直接跳转授权服务器
        // 此方法主要用于授权码回调后的处理，或密码模式（不推荐）

        log.info("OAuth2 authentication for user: {}", request.getUsername());

        // 这里可以实现密码模式（仅用于测试，生产环境不推荐）
        // 实际生产环境建议使用授权码模式

        return AuthenticationResult.failure(
                "OAuth2 模式请通过 /oauth2/authorization/{provider} 端点登录，" +
                        "或使用授权码回调接口 /oauth2/callback"
        );
    }

    /**
     * 用授权码换取 Token（供回调接口调用）
     */
    public AuthenticationResult exchangeCodeForToken(String code, String redirectUri) {
        try {
            AuthProperties.OAuth2Config oauth2Config = authProperties.getOauth2();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(oauth2Config.getClientId(), oauth2Config.getClientSecret());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    oauth2Config.getIssuerUri() + "/protocol/openid-connect/token",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                // 获取用户信息
                UserInfo userInfo = fetchUserInfo(accessToken);

                return AuthenticationResult.success(
                        accessToken,
                        refreshToken,
                        expiresIn != null ? expiresIn.longValue() : 3600L,
                        userInfo
                );
            }

            return AuthenticationResult.failure("OAuth2 token exchange failed");

        } catch (Exception e) {
            log.error("OAuth2 code exchange failed: {}", e.getMessage());
            return AuthenticationResult.failure("OAuth2 认证失败：" + e.getMessage());
        }
    }

    /**
     * 从 OAuth2 服务器获取用户信息
     */
    private UserInfo fetchUserInfo(String accessToken) {
        try {
            AuthProperties.OAuth2Config oauth2Config = authProperties.getOauth2();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    oauth2Config.getUserInfoUri(),
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();

                // 解析用户信息（根据标准 OIDC 规范）
                String sub = (String) userData.get("sub");
                String preferredUsername = (String) userData.get("preferred_username");
                String email = (String) userData.get("email");
                String name = (String) userData.get("name");

                // 角色映射
                // 从 OAuth2 服务器获取的角色映射到本地角色

                return UserInfo.builder()
                        .userId(0L) // OAuth2 用户可能不需要本地ID
                        .username(preferredUsername != null ? preferredUsername : sub)
                        .nickname(name)
                        .email(email)
                        .roles(Collections.singletonList("ADMIN")) // 简化处理
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to fetch user info: {}", e.getMessage());
        }

        return UserInfo.builder()
                .username("unknown")
                .roles(Collections.singletonList("USER"))
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        // 调用 OAuth2 服务器的 Token 验证端点
        // 或使用本地公钥验证 JWT 签名
        try {
            AuthProperties.OAuth2Config oauth2Config = authProperties.getOauth2();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    oauth2Config.getUserInfoUri(),
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UserDetails getUserDetails(String token) {
        UserInfo userInfo = fetchUserInfo(token);

        return UserDetails.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .roles(userInfo.getRoles())
                .enabled(true)
                .build();
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        try {
            AuthProperties.OAuth2Config oauth2Config = authProperties.getOauth2();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(oauth2Config.getClientId(), oauth2Config.getClientSecret());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    oauth2Config.getIssuerUri() + "/protocol/openid-connect/token",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                String newAccessToken = (String) tokenResponse.get("access_token");
                String newRefreshToken = (String) tokenResponse.get("refresh_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                UserInfo userInfo = fetchUserInfo(newAccessToken);

                return AuthenticationResult.success(
                        newAccessToken,
                        newRefreshToken,
                        expiresIn != null ? expiresIn.longValue() : 3600L,
                        userInfo
                );
            }

            return AuthenticationResult.failure("Token refresh failed");

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return AuthenticationResult.failure("刷新令牌失败：" + e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        // 可选：调用 OAuth2 服务器的 logout 端点
        log.info("OAuth2 logout");
    }

    @Override
    public String getAuthType() {
        return "oauth2";
    }
}
