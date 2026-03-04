package com.litegateway.admin.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * ==========================================
 * Lite Gateway 认证配置
 * ==========================================
 * 支持三种认证模式，通过 type 属性切换：
 *
 * 1. JWT 模式（默认）- 适合中小团队快速启动
 * 2. OAuth2 模式 - 适合已有统一认证中心的企业
 * 3. LDAP 模式 - 适合已有 AD/LDAP 的企业
 *
 * 切换方式：修改 lite.gateway.auth.type 配置后重启服务
 * ==========================================
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lite.gateway.auth")
public class AuthProperties {

    /**
     * 认证类型：jwt | oauth2 | ldap
     * 默认使用 JWT 内置认证
     */
    private String type = "jwt";

    /**
     * JWT 配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * OAuth2 配置
     */
    private OAuth2Config oauth2 = new OAuth2Config();

    /**
     * LDAP 配置
     */
    private LdapConfig ldap = new LdapConfig();

    /**
     * JWT 配置类
     */
    @Data
    public static class JwtConfig {
        /** JWT 签名密钥（生产环境务必修改！） */
        private String secret = "lite-gateway-secret-key-change-in-production";

        /** Access Token 有效期（毫秒），默认1小时 */
        private Long accessTokenExpiration = 3600000L;

        /** Refresh Token 有效期（毫秒），默认7天 */
        private Long refreshTokenExpiration = 604800000L;

        /** 是否允许多设备同时登录 */
        private Boolean allowMultiLogin = true;
    }

    /**
     * OAuth2 配置类
     */
    @Data
    public static class OAuth2Config {
        /** OAuth2 提供商类型：generic | keycloak | authing | okta */
        private String provider = "generic";

        /** 授权服务器地址 */
        private String issuerUri;

        /** JWT 公钥地址（用于验证 Token） */
        private String jwkSetUri;

        /** 客户端ID */
        private String clientId;

        /** 客户端密钥 */
        private String clientSecret;

        /** 用户信息端点 */
        private String userInfoUri;

        /** 角色映射（外部角色 -> 系统角色） */
        private Map<String, String> roleMapping = new HashMap<>();
    }

    /**
     * LDAP 配置类
     */
    @Data
    public static class LdapConfig {
        /** LDAP 服务器地址 */
        private String url;

        /** 基础 DN */
        private String baseDn;

        /** 管理员 DN */
        private String managerDn;

        /** 管理员密码 */
        private String managerPassword;

        /** 用户搜索基础 DN */
        private String userSearchBase = "ou=users";

        /** 用户搜索过滤器 */
        private String userSearchFilter = "(uid={0})";

        /** 组搜索基础 DN */
        private String groupSearchBase = "ou=groups";

        /** 组搜索过滤器 */
        private String groupSearchFilter = "(member={0})";

        /** 属性映射 */
        private Map<String, String> attributeMapping = new HashMap<>();
    }
}
