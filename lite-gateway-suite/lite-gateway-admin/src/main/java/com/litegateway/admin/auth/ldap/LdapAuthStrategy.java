package com.litegateway.admin.auth.ldap;

import com.litegateway.admin.auth.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;


import javax.naming.directory.Attributes;
import java.util.Collections;
import java.util.List;

/**
 * LDAP/AD 集成认证策略
 * 适合已有 Windows AD 或 OpenLDAP 的企业
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "lite.gateway.auth.type",
        havingValue = "ldap"
)
public class LdapAuthStrategy implements AuthStrategy {

    @Autowired
    private AuthProperties authProperties;

    private LdapTemplate ldapTemplate;

    @PostConstruct
    public void init() {
        AuthProperties.LdapConfig ldapConfig = authProperties.getLdap();

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapConfig.getUrl());
        contextSource.setBase(ldapConfig.getBaseDn());
        contextSource.setUserDn(ldapConfig.getManagerDn());
        contextSource.setPassword(ldapConfig.getManagerPassword());
        contextSource.afterPropertiesSet();

        ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);

        log.info("LDAP authentication initialized: {}", ldapConfig.getUrl());
    }

    @Override
    public AuthenticationResult authenticate(LoginRequest request) {
        log.info("LDAP authentication for user: {}", request.getUsername());

        try {
            AuthProperties.LdapConfig ldapConfig = authProperties.getLdap();

            // 构建用户过滤器
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "person"));
            filter.and(new EqualsFilter(
                    ldapConfig.getUserSearchFilter().replace("({0})", ""),
                    request.getUsername()
            ));

            // 搜索用户
            List<LdapUser> users = ldapTemplate.search(
                    ldapConfig.getUserSearchBase(),
                    filter.toString(),
                    new LdapUserAttributesMapper()
            );

            if (users.isEmpty()) {
                return AuthenticationResult.failure("用户不存在");
            }

            LdapUser ldapUser = users.get(0);

            // 验证密码（通过尝试绑定）
            boolean authenticated = authenticateLdapUser(
                    ldapUser.getDn(),
                    request.getPassword()
            );

            if (!authenticated) {
                return AuthenticationResult.failure("密码错误");
            }

            // 获取用户角色
            List<String> roles = fetchUserRoles(ldapUser.getDn());

            // 构建用户信息
            UserInfo userInfo = UserInfo.builder()
                    .userId(0L) // LDAP 用户可能没有数字ID
                    .username(ldapUser.getUsername())
                    .nickname(ldapUser.getRealName())
                    .email(ldapUser.getEmail())
                    .roles(roles)
                    .build();

            // LDAP 模式下，生成内部 JWT Token
            // 实际实现中应该复用 JwtTokenProvider
            String accessToken = "ldap-token-" + System.currentTimeMillis();
            String refreshToken = "ldap-refresh-" + System.currentTimeMillis();

            return AuthenticationResult.success(accessToken, refreshToken, 3600L, userInfo);

        } catch (Exception e) {
            log.error("LDAP authentication failed: {}", e.getMessage());
            return AuthenticationResult.failure("LDAP 认证失败：" + e.getMessage());
        }
    }

    /**
     * 验证 LDAP 用户密码
     */
    private boolean authenticateLdapUser(String userDn, String password) {
        try {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(authProperties.getLdap().getUrl());
            contextSource.setUserDn(userDn);
            contextSource.setPassword(password);
            contextSource.afterPropertiesSet();

            LdapTemplate authTemplate = new LdapTemplate(contextSource);
            authTemplate.authenticate("", "(objectClass=*)", password);

            return true;
        } catch (Exception e) {
            log.error("LDAP bind failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户角色
     */
    private List<String> fetchUserRoles(String userDn) {
        try {
            AuthProperties.LdapConfig ldapConfig = authProperties.getLdap();

            // 搜索用户所属组
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "groupOfNames"));
            filter.and(new EqualsFilter("member", userDn));

            List<String> groups = ldapTemplate.search(
                    ldapConfig.getGroupSearchBase(),
                    filter.toString(),
                    (AttributesMapper<String>) attrs -> attrs.get("cn").get().toString()
            );

            // 映射为角色
            return groups.isEmpty() ? Collections.singletonList("USER") : groups;

        } catch (Exception e) {
            log.error("Failed to fetch LDAP groups: {}", e.getMessage());
            return Collections.singletonList("USER");
        }
    }

    @Override
    public boolean validateToken(String token) {
        // LDAP 模式下，Token 是内部生成的 JWT
        // 实际应该调用 JwtTokenProvider 验证
        return token != null && token.startsWith("ldap-token-");
    }

    @Override
    public UserDetails getUserDetails(String token) {
        // 从 Token 解析用户信息
        // 简化实现
        return UserDetails.builder()
                .username("ldap-user")
                .roles(Collections.singletonList("USER"))
                .enabled(true)
                .build();
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        // 简化实现
        return AuthenticationResult.failure("LDAP 模式暂不支持 Token 刷新");
    }

    @Override
    public void logout(String token) {
        log.info("LDAP logout");
    }

    @Override
    public String getAuthType() {
        return "ldap";
    }

    /**
     * LDAP 用户属性映射器
     */
    private static class LdapUserAttributesMapper implements AttributesMapper<LdapUser> {
        @Override
        public LdapUser mapFromAttributes(Attributes attrs) throws javax.naming.NamingException {
            LdapUser user = new LdapUser();
            user.setDn(attrs.get("dn").get().toString());
            user.setUsername(attrs.get("uid").get().toString());
            user.setRealName(attrs.get("cn") != null ? attrs.get("cn").get().toString() : "");
            user.setEmail(attrs.get("mail") != null ? attrs.get("mail").get().toString() : "");
            return user;
        }
    }

    /**
     * LDAP 用户内部类
     */
    private static class LdapUser {
        private String dn;
        private String username;
        private String realName;
        private String email;

        // Getters and setters
        public String getDn() { return dn; }
        public void setDn(String dn) { this.dn = dn; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
