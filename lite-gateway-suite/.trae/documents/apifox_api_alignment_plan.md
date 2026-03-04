# APIFox 接口对齐计划

## 目标
基于 APIFox 生成的接口规范 `https://s.apifox.cn/eb03918d-57c2-4a90-97ab-a9654e3fa593`，完善 `lite-gateway-ui` 前端项目的接口和 TypeScript 类型定义，确保前后端接口一致性。

## 后端接口分析

### 1. 认证相关接口 (AuthController.java)

| 接口 | 方法 | 请求路径 | 请求体 | 响应体 |
|------|------|----------|--------|--------|
| 用户登录 | POST | /auth/login | LoginRequest | Result<LoginResponse> |
| 刷新 Token | POST | /auth/refresh | RefreshRequest | Result<LoginResponse> |
| 获取用户信息 | GET | /auth/user-info | - | Result<UserInfo> |
| 用户登出 | POST | /auth/logout | - | Result<Void> |
| 获取认证配置 | GET | /auth/config | - | Result<AuthConfigResponse> |

### 2. 数据模型对比

#### 后端 LoginRequest (Java)
```java
@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
    private String captcha;      // 可选
    private String captchaKey;   // 可选
}
```

#### 前端 LoginParams (TypeScript) - 需要更新
```typescript
export interface LoginParams {
  username: string
  password: string
  captcha?: string      // 新增
  captchaKey?: string   // 新增
}
```

#### 后端 LoginResponse (Java)
```java
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo userInfo;
}
```

#### 后端 UserInfo (Java)
```java
@Data
@Builder
public class UserInfo {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
}
```

#### 前端 LoginResult (TypeScript) - 需要更新
```typescript
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo      // 需要新的 UserInfo 类型
}

export interface UserInfo {
  userId: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}
```

#### 后端 Result<T> (Java)
```java
@Data
public class Result<T> {
    private String code;      // 注意：后端是 String 类型
    private String message;
    private T data;
}
```

#### 前端 ApiResponse<T> (TypeScript) - 需要更新
```typescript
export interface ApiResponse<T = any> {
  code: string    // 改为 string 类型以匹配后端
  message: string
  data: T
}
```

### 3. 统一响应码

后端 ErrorCodeEnum 使用字符串类型错误码：
- `00000` - 成功
- `A0201` - 用户账户不存在
- `A0210` - 用户密码错误
- `A0230` - 用户登录已过期
- 等等...

## 实施步骤

### 步骤 1: 更新 TypeScript 类型定义 (types/api.ts)

1. 修改 `ApiResponse` 接口 - `code` 从 `number` 改为 `string`
2. 更新 `LoginParams` 接口 - 添加 `captcha` 和 `captchaKey` 可选字段
3. 更新 `LoginResult` 接口 - 完善字段，使用新的 `UserInfo` 类型
4. 创建新的 `UserInfo` 接口 - 匹配后端 UserInfo 结构
5. 更新 `SysUser` 接口 - 与 UserInfo 保持一致

### 步骤 2: 更新用户 API (api/user.ts)

1. 添加 `/auth/config` 接口调用
2. 更新 login 方法返回类型
3. 确保所有方法使用正确的类型

### 步骤 3: 更新请求工具 (utils/request.ts)

1. 更新响应拦截器处理逻辑，适配字符串类型的 code
2. 确保正确处理后端 Result 包装结构

### 步骤 4: 更新错误处理服务 (services/errorConfigService.ts)

1. 更新 `isSuccessCode` 函数，使用字符串比较
2. 确保错误码映射使用字符串类型

### 步骤 5: 更新用户 Store (store/modules/user.ts)

1. 更新 `userInfo` 类型为新的 `UserInfo`
2. 确保登录后正确处理 `userInfo` 数据

### 步骤 6: 更新登录页面 (views/login/index.vue)

1. 添加验证码输入框（可选）
2. 更新表单提交逻辑

## 验证清单

- [ ] `ApiResponse.code` 类型为 `string`
- [ ] `LoginParams` 包含 `captcha` 和 `captchaKey` 可选字段
- [ ] `LoginResult` 结构与后端 `LoginResponse` 一致
- [ ] `UserInfo` 接口包含所有后端字段
- [ ] 请求拦截器正确处理 `Bearer Token`
- [ ] 响应拦截器正确处理字符串类型的 code
- [ ] 登录页面可以正常调用后端接口
- [ ] 登录成功后正确存储 token 和 userInfo
- [ ] 获取用户信息接口返回正确数据

## 文件变更列表

1. `src/types/api.ts` - 更新类型定义
2. `src/api/user.ts` - 更新 API 方法
3. `src/utils/request.ts` - 更新响应处理
4. `src/services/errorConfigService.ts` - 更新错误码处理
5. `src/store/modules/user.ts` - 更新状态管理
6. `src/views/login/index.vue` - 更新登录页面（可选）
