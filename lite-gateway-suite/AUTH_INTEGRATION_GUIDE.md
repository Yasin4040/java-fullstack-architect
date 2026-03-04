# 🔐 Lite Gateway 认证集成指南

## 概述

Lite Gateway Admin 模块支持三种认证模式，通过配置即可切换：

| 认证模式 | 适用场景 | 配置复杂度 | 推荐度 |
|---------|---------|-----------|-------|
| **JWT（默认）** | 中小团队快速启动 | ⭐ | ⭐⭐⭐⭐⭐ |
| **OAuth2** | 已有统一认证中心的企业 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **LDAP/AD** | 已有 Windows AD 或 OpenLDAP | ⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 快速决策

```
你是哪种情况？
│
├─ 5人小团队，想快速启动？
│  └─> 使用 JWT（默认，无需配置）
│
├─ 已有 Spring Security OAuth2 / Keycloak / Authing？
│  └─> 使用 OAuth2 模式
│
├─ 已有 Windows AD / OpenLDAP？
│  └─> 使用 LDAP 模式
│
└─ 想自定义认证逻辑？
   └─> 实现 AuthStrategy 接口
```

---

## 模式一：JWT 内置认证（默认）

### 特点
- ✅ 零外部依赖，开箱即用
- ✅ 支持双 Token（Access + Refresh）
- ✅ 支持多设备登录
- ✅ 适合中小团队

### 配置
```yaml
lite:
  gateway:
    auth:
      type: jwt
      jwt:
        secret: your-strong-secret-key-here  # 生产环境务必修改！
        access-token-expiration: 3600000     # 1小时
        refresh-token-expiration: 604800000  # 7天
```

### 默认账号
```
用户名：admin
密码：123456
```

### 切换命令
```bash
# 无需额外配置，直接启动
java -jar lite-gateway-admin.jar
```

---

## 模式二：OAuth2 集成

### 支持的提供商
- Spring Security OAuth2
- Keycloak
- Authing
- Okta
- 任何标准 OIDC 提供商

### 2.1 集成 Keycloak

```bash
# 环境变量配置
export AUTH_TYPE=oauth2
export OAUTH2_PROVIDER=keycloak
export OAUTH2_ISSUER_URI=http://keycloak:8080/realms/my-realm
export OAUTH2_CLIENT_ID=lite-gateway
export OAUTH2_CLIENT_SECRET=your-client-secret
```

### 2.2 集成 Authing

```bash
export AUTH_TYPE=oauth2
export OAUTH2_PROVIDER=authing
export OAUTH2_ISSUER_URI=https://your-app.authing.cn/oidc
export OAUTH2_CLIENT_ID=your-client-id
export OAUTH2_CLIENT_SECRET=your-client-secret
```

### 2.3 配置文件方式

```yaml
lite:
  gateway:
    auth:
      type: oauth2
      oauth2:
        provider: keycloak  # generic | keycloak | authing | okta
        issuer-uri: http://keycloak:8080/realms/my-realm
        client-id: lite-gateway
        client-secret: your-secret
        user-info-uri: http://keycloak:8080/realms/my-realm/protocol/openid-connect/userinfo
        role-mapping:
          ADMIN: gateway-admin
          OPERATOR: gateway-operator
```

### 前端适配

OAuth2 模式下，前端需要调整登录流程：

```typescript
// 1. 获取认证配置
const authConfig = await getAuthConfig();

// 2. 根据配置选择登录方式
if (authConfig.authType === 'oauth2') {
  // 跳转 OAuth2 授权页
  window.location.href = authConfig.oauth2AuthorizationUri;
} else {
  // 显示本地登录表单
  showLoginForm();
}

// 3. 处理 OAuth2 回调
// 回调地址：/oauth2/callback?code=xxx
async function handleOAuth2Callback(code: string) {
  const result = await exchangeCodeForToken(code);
  saveToken(result.accessToken);
}
```

---

## 模式三：LDAP/AD 集成

### 适用场景
- 已有 Windows Active Directory
- 已有 OpenLDAP
- 希望通过域账号登录

### 配置

```yaml
lite:
  gateway:
    auth:
      type: ldap
      ldap:
        url: ldap://ad.company.com:389
        base-dn: dc=company,dc=com
        manager-dn: cn=admin,dc=company,dc=com
        manager-password: admin-password
        user-search-base: ou=users
        user-search-filter: (uid={0})
        group-search-base: ou=groups
        group-search-filter: (member={0})
        attribute-mapping:
          username: uid
          email: mail
          real-name: cn
```

### 环境变量方式

```bash
export AUTH_TYPE=ldap
export LDAP_URL=ldap://ad.company.com:389
export LDAP_BASE_DN=dc=company,dc=com
export LDAP_MANAGER_DN=cn=admin,dc=company,dc=com
export LDAP_MANAGER_PASSWORD=admin-password
```

---

## 🔧 高级：自定义认证策略

如果以上模式都不满足需求，可以实现自定义策略：

### 1. 实现 AuthStrategy 接口

```java
@Component
@ConditionalOnProperty(name = "lite.gateway.auth.type", havingValue = "custom")
public class CustomAuthStrategy implements AuthStrategy {

    @Override
    public AuthenticationResult authenticate(LoginRequest request) {
        // 你的自定义逻辑
    }

    @Override
    public boolean validateToken(String token) {
        // 验证逻辑
    }

    @Override
    public UserDetails getUserDetails(String token) {
        // 获取用户信息
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        // 刷新逻辑
    }

    @Override
    public void logout(String token) {
        // 登出逻辑
    }

    @Override
    public String getAuthType() {
        return "custom";
    }
}
```

### 2. 启用自定义策略

```yaml
lite:
  gateway:
    auth:
      type: custom  # 自动加载你的实现
```

---

## ✅ 验证集成成功

### 1. 检查认证配置

启动后访问：
```bash
curl http://localhost:8080/auth/config
```

返回示例（JWT 模式）：
```json
{
  "code": "200",
  "data": {
    "authType": "jwt",
    "oauth2Enabled": false,
    "ldapEnabled": false
  }
}
```

返回示例（OAuth2 模式）：
```json
{
  "code": "200",
  "data": {
    "authType": "oauth2",
    "oauth2Enabled": true,
    "ldapEnabled": false,
    "oauth2AuthorizationUri": "http://keycloak:8080/realms/my-realm/protocol/openid-connect/auth?client_id=lite-gateway&response_type=code&redirect_uri=http://localhost:8081/oauth2/callback"
  }
}
```

### 2. 测试登录

```bash
# JWT 模式
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 返回
{
  "code": "200",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userInfo": {
      "userId": 1,
      "username": "admin",
      "roles": ["ADMIN"]
    }
  }
}
```

### 3. 测试受保护接口

```bash
curl http://localhost:8080/system/user/list \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

---

## 🚀 生产环境建议

### 1. JWT 密钥安全
```bash
# 生成强密钥
openssl rand -base64 32

# 设置为环境变量
export JWT_SECRET=your-generated-key-here
```

### 2. HTTPS 强制
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your-password
    key-store-type: PKCS12
```

### 3. 密码加密
默认使用 BCrypt，如需调整：
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // 强度 12
}
```

---

## 📚 常见问题

### Q: 如何切换认证模式？
A: 修改 `lite.gateway.auth.type` 配置，重启服务即可。

### Q: OAuth2 模式下还需要用户表吗？
A: 可选。可以只同步必要信息到本地，或完全依赖外部系统。

### Q: 支持多因素认证（MFA）吗？
A: JWT/LDAP 模式下可在 `authenticate` 方法中扩展。OAuth2 模式下由外部提供商处理。

### Q: Token 黑名单如何实现？
A: 在 `logout` 方法中将 Token 加入 Redis 黑名单，在 `validateToken` 中检查。

---

## 📞 需要帮助？

- 查看源码：`com.litegateway.admin.auth` 包
- 查看配置：`application.yml` 中的 `lite.gateway.auth` 部分
- 提交 Issue：项目 GitHub Issues
