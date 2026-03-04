# IdentityProvider 接口设计 Spec

## 设计目标
剥离原企业多渠道登录的授权鉴权逻辑，抽象成通用的 IdentityProvider 接口，支持：
1. 多种登录渠道（账号密码、微信、钉钉、LDAP、OAuth2 等）
2. 默认 JWT Token 实现
3. 可插拔的认证提供者架构
4. 统一的用户信息模型

---

## 核心接口设计

### 1. IdentityProvider 接口（认证提供者核心接口）

```java
/**
 * 身份认证提供者接口
 * 支持多种登录渠道的抽象
 */
public interface IdentityProvider {
    
    /**
     * 获取提供者唯一标识
     * 如：jwt、wechat、dingtalk、ldap、oauth2 等
     */
    String getProviderId();
    
    /**
     * 获取提供者名称
     */
    String getProviderName();
    
    /**
     * 是否支持该类型的认证请求
     */
    boolean supports(AuthenticationRequest request);
    
    /**
     * 执行认证
     * @param request 认证请求
     * @return 认证结果
     */
    AuthenticationResult authenticate(AuthenticationRequest request);
    
    /**
     * 验证 Token 有效性
     * @param token 访问令牌
     * @return 验证结果
     */
    TokenValidationResult validateToken(String token);
    
    /**
     * 刷新 Token
     * @param refreshToken 刷新令牌
     * @return 新的认证结果
     */
    AuthenticationResult refreshToken(String refreshToken);
    
    /**
     * 注销登录
     * @param token 访问令牌
     */
    void logout(String token);
}
```

### 2. 认证请求模型

```java
/**
 * 认证请求基类
 */
@Data
public class AuthenticationRequest {
    
    /**
     * 认证类型
     * 如：password、wechat_code、dingtalk_code、ldap、oauth2 等
     */
    private String authType;
    
    /**
     * 客户端 ID
     */
    private String clientId;
    
    /**
     * 客户端密钥
     */
    private String clientSecret;
    
    /**
     * 扩展参数
     * 不同渠道需要不同的参数
     */
    private Map<String, Object> additionalParams;
    
    /**
     * 请求 IP
     */
    private String requestIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
}

/**
 * 账号密码认证请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UsernamePasswordRequest extends AuthenticationRequest {
    
    private String username;
    private String password;
    
    public UsernamePasswordRequest() {
        setAuthType("password");
    }
}

/**
 * 微信扫码认证请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WechatAuthRequest extends AuthenticationRequest {
    
    private String code;
    private String state;
    
    public WechatAuthRequest() {
        setAuthType("wechat");
    }
}

/**
 * 钉钉认证请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingTalkAuthRequest extends AuthenticationRequest {
    
    private String authCode;
    private String corpId;
    
    public DingTalkAuthRequest() {
        setAuthType("dingtalk");
    }
}
```

### 3. 认证结果模型

```java
/**
 * 认证结果
 */
@Data
@Builder
public class AuthenticationResult {
    
    /**
     * 是否认证成功
     */
    private boolean success;
    
    /**
     * 错误码（失败时）
     */
    private String errorCode;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * Token 类型，如：Bearer
     */
    private String tokenType;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserIdentity userIdentity;
    
    /**
     * 额外信息
     */
    private Map<String, Object> additionalInfo;
}
```

### 4. 统一用户身份模型

```java
/**
 * 用户身份信息
 * 统一的跨渠道用户模型
 */
@Data
@Builder
public class UserIdentity {
    
    /**
     * 用户唯一标识（系统内部 ID）
     */
    private String userId;
    
    /**
     * 用户名/账号
     */
    private String username;
    
    /**
     * 昵称/显示名称
     */
    private String nickname;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像 URL
     */
    private String avatar;
    
    /**
     * 认证来源
     * 如：jwt、wechat、dingtalk、ldap
     */
    private String authSource;
    
    /**
     * 来源用户 ID（第三方系统的用户 ID）
     */
    private String sourceUserId;
    
    /**
     * 用户角色列表
     */
    private List<String> roles;
    
    /**
     * 用户权限列表
     */
    private List<String> permissions;
    
    /**
     * 部门信息
     */
    private DepartmentInfo department;
    
    /**
     * 扩展属性
     * 不同渠道可能有不同的扩展信息
     */
    private Map<String, Object> extensions;
    
    /**
     * 账号状态
     */
    private AccountStatus status;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 最后登录 IP
     */
    private String lastLoginIp;
}

/**
 * 部门信息
 */
@Data
public class DepartmentInfo {
    private String deptId;
    private String deptName;
    private String parentDeptId;
    private String deptPath;
}

/**
 * 账号状态
 */
public enum AccountStatus {
    ACTIVE,      // 正常
    LOCKED,      // 锁定
    DISABLED,    // 禁用
    EXPIRED      // 过期
}
```

---

## 默认 JWT 实现

### 1. JwtIdentityProvider

```java
/**
 * JWT 默认身份认证提供者
 */
@Component
@ConditionalOnProperty(name = "identity.provider.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class JwtIdentityProvider implements IdentityProvider {
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public String getProviderId() {
        return "jwt";
    }
    
    @Override
    public String getProviderName() {
        return "JWT Token Provider";
    }
    
    @Override
    public boolean supports(AuthenticationRequest request) {
        return "password".equals(request.getAuthType()) || 
               "jwt".equals(request.getAuthType());
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        // 1. 验证请求类型
        if (!(request instanceof UsernamePasswordRequest)) {
            return AuthenticationResult.builder()
                    .success(false)
                    .errorCode("UNSUPPORTED_REQUEST")
                    .errorMessage("不支持的认证请求类型")
                    .build();
        }
        
        UsernamePasswordRequest pwdRequest = (UsernamePasswordRequest) request;
        
        try {
            // 2. 加载用户信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(pwdRequest.getUsername());
            
            // 3. 验证密码
            if (!passwordEncoder.matches(pwdRequest.getPassword(), userDetails.getPassword())) {
                return AuthenticationResult.builder()
                        .success(false)
                        .errorCode("INVALID_CREDENTIALS")
                        .errorMessage("用户名或密码错误")
                        .build();
            }
            
            // 4. 构建用户身份
            UserIdentity userIdentity = buildUserIdentity(userDetails);
            
            // 5. 生成 Token
            String accessToken = jwtTokenService.generateAccessToken(userIdentity);
            String refreshToken = jwtTokenService.generateRefreshToken(userIdentity);
            
            // 6. 返回结果
            return AuthenticationResult.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenService.getAccessTokenExpiration())
                    .userIdentity(userIdentity)
                    .build();
                    
        } catch (UsernameNotFoundException e) {
            return AuthenticationResult.builder()
                    .success(false)
                    .errorCode("USER_NOT_FOUND")
                    .errorMessage("用户不存在")
                    .build();
        }
    }
    
    @Override
    public TokenValidationResult validateToken(String token) {
        return jwtTokenService.validateToken(token);
    }
    
    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        // 1. 验证刷新令牌
        TokenValidationResult validation = jwtTokenService.validateToken(refreshToken);
        if (!validation.isValid()) {
            return AuthenticationResult.builder()
                    .success(false)
                    .errorCode("INVALID_REFRESH_TOKEN")
                    .errorMessage("刷新令牌无效或已过期")
                    .build();
        }
        
        // 2. 获取用户信息
        String userId = validation.getUserId();
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        UserIdentity userIdentity = buildUserIdentity(userDetails);
        
        // 3. 生成新 Token
        String newAccessToken = jwtTokenService.generateAccessToken(userIdentity);
        String newRefreshToken = jwtTokenService.generateRefreshToken(userIdentity);
        
        return AuthenticationResult.builder()
                .success(true)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getAccessTokenExpiration())
                .userIdentity(userIdentity)
                .build();
    }
    
    @Override
    public void logout(String token) {
        jwtTokenService.revokeToken(token);
    }
    
    private UserIdentity buildUserIdentity(UserDetails userDetails) {
        // 转换 UserDetails 为 UserIdentity
        return UserIdentity.builder()
                .userId(userDetails.getUsername())
                .username(userDetails.getUsername())
                .authSource("jwt")
                .roles(userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
```

### 2. JwtTokenService

```java
/**
 * JWT Token 服务
 */
public interface JwtTokenService {
    
    /**
     * 生成访问令牌
     */
    String generateAccessToken(UserIdentity userIdentity);
    
    /**
     * 生成刷新令牌
     */
    String generateRefreshToken(UserIdentity userIdentity);
    
    /**
     * 验证 Token
     */
    TokenValidationResult validateToken(String token);
    
    /**
     * 撤销 Token
     */
    void revokeToken(String token);
    
    /**
     * 从 Token 解析用户身份
     */
    UserIdentity parseToken(String token);
    
    /**
     * 获取访问令牌过期时间（秒）
     */
    Long getAccessTokenExpiration();
}

/**
 * Token 验证结果
 */
@Data
@Builder
public class TokenValidationResult {
    private boolean valid;
    private String userId;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> claims;
}
```

---

## 多渠道登录扩展实现

### 1. 微信登录提供者

```java
/**
 * 微信身份认证提供者
 */
@Component
@ConditionalOnProperty(name = "identity.provider.wechat.enabled", havingValue = "true")
public class WechatIdentityProvider implements IdentityProvider {
    
    @Autowired
    private WechatApiClient wechatApiClient;
    
    @Autowired
    private UserBindService userBindService;
    
    @Override
    public String getProviderId() {
        return "wechat";
    }
    
    @Override
    public String getProviderName() {
        return "WeChat Login";
    }
    
    @Override
    public boolean supports(AuthenticationRequest request) {
        return "wechat".equals(request.getAuthType());
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        WechatAuthRequest wechatRequest = (WechatAuthRequest) request;
        
        // 1. 调用微信接口获取用户信息
        WechatUserInfo wechatUser = wechatApiClient.getUserInfo(wechatRequest.getCode());
        
        // 2. 查找或绑定系统用户
        UserIdentity userIdentity = userBindService.findOrBindUser("wechat", wechatUser.getOpenid(), 
                () -> convertToUserIdentity(wechatUser));
        
        // 3. 生成系统内部 Token
        String accessToken = generateSystemToken(userIdentity);
        String refreshToken = generateRefreshToken(userIdentity);
        
        return AuthenticationResult.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userIdentity(userIdentity)
                .build();
    }
    
    // ... 其他方法实现
}
```

### 2. LDAP 登录提供者

```java
/**
 * LDAP 身份认证提供者
 */
@Component
@ConditionalOnProperty(name = "identity.provider.ldap.enabled", havingValue = "true")
public class LdapIdentityProvider implements IdentityProvider {
    
    @Autowired
    private LdapTemplate ldapTemplate;
    
    @Override
    public String getProviderId() {
        return "ldap";
    }
    
    @Override
    public boolean supports(AuthenticationRequest request) {
        return "ldap".equals(request.getAuthType());
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        UsernamePasswordRequest pwdRequest = (UsernamePasswordRequest) request;
        
        // 1. LDAP 认证
        boolean authenticated = ldapTemplate.authenticate(
                "ou=users", 
                "uid=" + pwdRequest.getUsername(), 
                pwdRequest.getPassword()
        );
        
        if (!authenticated) {
            return AuthenticationResult.builder()
                    .success(false)
                    .errorCode("LDAP_AUTH_FAILED")
                    .errorMessage("LDAP 认证失败")
                    .build();
        }
        
        // 2. 查询 LDAP 用户信息
        UserIdentity userIdentity = queryLdapUser(pwdRequest.getUsername());
        
        // 3. 生成 Token
        // ...
    }
}
```

---

## 认证管理器

### IdentityProviderManager

```java
/**
 * 身份认证提供者管理器
 * 负责管理多个认证提供者，根据请求类型路由到对应的提供者
 */
@Component
public class IdentityProviderManager {
    
    private final List<IdentityProvider> providers;
    private final IdentityProvider defaultProvider;
    
    @Autowired
    public IdentityProviderManager(List<IdentityProvider> providers) {
        this.providers = providers;
        this.defaultProvider = providers.stream()
                .filter(p -> "jwt".equals(p.getProviderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("默认 JWT Provider 未找到"));
    }
    
    /**
     * 执行认证
     */
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        // 1. 查找支持的提供者
        IdentityProvider provider = findProvider(request);
        
        // 2. 执行认证
        return provider.authenticate(request);
    }
    
    /**
     * 验证 Token
     */
    public TokenValidationResult validateToken(String token) {
        // 尝试所有提供者验证，直到成功
        for (IdentityProvider provider : providers) {
            TokenValidationResult result = provider.validateToken(token);
            if (result.isValid()) {
                return result;
            }
        }
        return TokenValidationResult.builder()
                .valid(false)
                .errorCode("INVALID_TOKEN")
                .errorMessage("Token 无效")
                .build();
    }
    
    /**
     * 刷新 Token
     */
    public AuthenticationResult refreshToken(String refreshToken) {
        // 根据 Token 类型路由到对应提供者
        IdentityProvider provider = resolveProviderByToken(refreshToken);
        return provider.refreshToken(refreshToken);
    }
    
    /**
     * 注销
     */
    public void logout(String token) {
        IdentityProvider provider = resolveProviderByToken(token);
        provider.logout(token);
    }
    
    private IdentityProvider findProvider(AuthenticationRequest request) {
        return providers.stream()
                .filter(p -> p.supports(request))
                .findFirst()
                .orElse(defaultProvider);
    }
    
    private IdentityProvider resolveProviderByToken(String token) {
        // 根据 Token 内容判断属于哪个提供者
        // 可以通过 Token 前缀、JWT payload 中的 issuer 等判断
        // 简化处理：先尝试默认提供者
        return defaultProvider;
    }
}
```

---

## Spring Security 集成

### 1. 自定义 AuthenticationProvider

```java
/**
 * Spring Security 集成的认证提供者
 */
@Component
public class IdentityAuthenticationProvider implements AuthenticationProvider {
    
    @Autowired
    private IdentityProviderManager identityProviderManager;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 1. 转换 Spring Security 的认证请求为 Identity 认证请求
        AuthenticationRequest request = convertToRequest(authentication);
        
        // 2. 调用 IdentityProviderManager 执行认证
        AuthenticationResult result = identityProviderManager.authenticate(request);
        
        // 3. 转换认证结果为 Spring Security 的 Authentication
        if (result.isSuccess()) {
            return createSuccessAuthentication(result);
        } else {
            throw new BadCredentialsException(result.getErrorMessage());
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    private AuthenticationRequest convertToRequest(Authentication authentication) {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        UsernamePasswordRequest request = new UsernamePasswordRequest();
        request.setUsername(token.getName());
        request.setPassword(token.getCredentials().toString());
        return request;
    }
    
    private Authentication createSuccessAuthentication(AuthenticationResult result) {
        UserIdentity user = result.getUserIdentity();
        
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        IdentityAuthenticationToken authentication = new IdentityAuthenticationToken(
                user, 
                result.getAccessToken(), 
                authorities
        );
        authentication.setDetails(result);
        
        return authentication;
    }
}
```

### 2. JWT 过滤器

```java
/**
 * JWT Token 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private IdentityProviderManager identityProviderManager;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        
        // 1. 从请求头获取 Token
        String token = extractToken(request);
        
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 2. 验证 Token
            TokenValidationResult validation = identityProviderManager.validateToken(token);
            
            if (validation.isValid()) {
                // 3. 设置认证信息到 SecurityContext
                Authentication authentication = createAuthentication(validation);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private Authentication createAuthentication(TokenValidationResult validation) {
        // 构建 Spring Security 的 Authentication 对象
        // ...
    }
}
```

---

## 配置示例

### application.yml

```yaml
identity:
  provider:
    # JWT 默认配置
    jwt:
      enabled: true
      secret: your-secret-key-here
      access-token-expiration: 3600  # 1小时
      refresh-token-expiration: 86400  # 24小时
      issuer: gateway-platform
    
    # 微信登录配置
    wechat:
      enabled: true
      app-id: your-wechat-app-id
      app-secret: your-wechat-app-secret
    
    # 钉钉登录配置
    dingtalk:
      enabled: true
      app-key: your-dingtalk-app-key
      app-secret: your-dingtalk-app-secret
    
    # LDAP 配置
    ldap:
      enabled: false
      url: ldap://localhost:389
      base-dn: dc=example,dc=com
      user-dn-pattern: uid={0},ou=users
```

---

## 使用示例

### 1. 账号密码登录

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private IdentityProviderManager identityProviderManager;
    
    @PostMapping("/login")
    public Result<AuthenticationResult> login(@RequestBody LoginRequest loginRequest) {
        // 构建认证请求
        UsernamePasswordRequest request = new UsernamePasswordRequest();
        request.setUsername(loginRequest.getUsername());
        request.setPassword(loginRequest.getPassword());
        request.setClientId(loginRequest.getClientId());
        request.setRequestIp(getClientIp());
        
        // 执行认证
        AuthenticationResult result = identityProviderManager.authenticate(request);
        
        if (result.isSuccess()) {
            return Result.ok(result);
        } else {
            return Result.fail(result.getErrorCode(), result.getErrorMessage());
        }
    }
}
```

### 2. 微信扫码登录

```java
@GetMapping("/login/wechat")
public Result<AuthenticationResult> wechatLogin(@RequestParam String code) {
    WechatAuthRequest request = new WechatAuthRequest();
    request.setCode(code);
    
    AuthenticationResult result = identityProviderManager.authenticate(request);
    return Result.ok(result);
}
```

### 3. 自定义认证提供者扩展

```java
/**
 * 自定义认证提供者示例
 */
@Component
public class CustomIdentityProvider implements IdentityProvider {
    
    @Override
    public String getProviderId() {
        return "custom";
    }
    
    @Override
    public boolean supports(AuthenticationRequest request) {
        return "custom".equals(request.getAuthType());
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        // 实现自定义认证逻辑
        // ...
    }
    
    // ... 其他方法
}
```

---

## 总结

这套 IdentityProvider 接口设计的核心优势：

1. **统一抽象**：所有认证渠道都实现相同的 IdentityProvider 接口
2. **默认 JWT**：开箱即用的 JWT 实现，无需额外配置即可使用
3. **可扩展**：通过添加新的 IdentityProvider 实现即可支持新渠道
4. **解耦**：业务代码只依赖 IdentityProviderManager，不感知具体实现
5. **Spring Security 集成**：无缝集成 Spring Security 生态
