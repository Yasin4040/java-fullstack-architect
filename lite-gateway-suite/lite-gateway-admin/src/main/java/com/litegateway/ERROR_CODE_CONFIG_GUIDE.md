# Lite Gateway 统一错误码配置指南

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [配置文件](#配置文件)
4. [错误码规范](#错误码规范)
5. [后端实现](#后端实现)
6. [前端实现](#前端实现)
7. [配置流程](#配置流程)
8. [使用示例](#使用示例)
9. [注意事项](#注意事项)

---

## 概述

Lite Gateway 采用统一的错误码管理机制，通过 YAML 配置文件集中管理所有错误码和成功码，实现前后端错误处理的一致性和可维护性。

### 核心特性

- ✅ **配置化**：所有错误码集中配置在 YAML 文件中
- ✅ **可扩展**：支持自定义错误码和成功码
- ✅ **前后端统一**：后端配置自动同步到前端
- ✅ **分级处理**：支持不同级别的错误处理策略
- ✅ **动作支持**：支持登出、跳转、重试等动作

---

## 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           统一错误码架构                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   配置层 (YAML)                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  error-codes.yml                                                    │  │
│   │  ├── 成功码 (success: true)                                         │  │
│   │  ├── 错误码 (按 A/B/C/G 分类)                                       │  │
│   │  └── 动作配置 (logout/redirect/retry)                               │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                              │                                              │
│                              ▼                                              │
│   后端 (Java)                                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  ErrorCodeService                                                   │  │
│   │  ├── 加载 YAML 配置                                                 │  │
│   │  ├── 设置成功码到 Result 类                                         │  │
│   │  └── 提供 /api/config/error-codes 接口                              │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                              │                                              │
│                              │  HTTP 请求                                    │
│                              ▼                                              │
│   前端 (Vue3)                                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  errorConfigService                                                 │  │
│   │  ├── 应用启动时拉取配置                                             │  │
│   │  ├── 缓存成功码和错误码                                             │  │
│   │  └── 提供 isSuccessCode() 方法                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                              │                                              │
│                              ▼                                              │
│   业务层                                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  request.ts (拦截器)                                                │  │
│   │  ├── 使用配置的成功码判断响应                                       │  │
│   │  └── 错误时调用 useErrorHandler                                     │  │
│   │                                                                     │  │
│   │  useErrorHandler (Hook)                                             │  │
│   │  ├── 根据错误码配置显示通知                                         │  │
│   │  └── 执行配置的动作 (logout/redirect)                               │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 数据流转图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           数据流转流程                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   阶段 1: 应用启动                                                           │
│   ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐          │
│   │  后端    │     │ 加载 YAML │     │ 设置成功码│     │ 启动完成 │          │
│   │  启动    │────▶│  配置    │────▶│ 到 Result│────▶│         │          │
│   └──────────┘     └──────────┘     └──────────┘     └──────────┘          │
│        │                                                    │               │
│        │                                                    │               │
│        │  阶段 2: 前端启动                                    │               │
│        │  ┌──────────┐     ┌──────────┐     ┌──────────┐    │               │
│        └──▶│ 前端    │────▶│ 请求配置  │────▶│ 缓存配置 │────┘               │
│           │  启动    │     │  接口    │     │         │                    │
│           └──────────┘     └──────────┘     └──────────┘                    │
│                                                    │                        │
│        ┌───────────────────────────────────────────┘                        │
│        │                                                                    │
│        │  阶段 3: 业务请求                                                    │
│        │  ┌──────────┐     ┌──────────┐     ┌──────────┐                    │
│        └──▶│ 业务    │────▶│ 后端返回 │────▶│ 前端判断 │                    │
│           │  请求    │     │  响应    │     │ 成功/失败│                    │
│           └──────────┘     └──────────┘     └────┬─────┘                    │
│                                                  │                          │
│                     ┌────────────────────────────┼─────────────────────┐    │
│                     │                            │                     │    │
│                     ▼                            ▼                     │    │
│              ┌──────────┐                 ┌──────────┐                 │    │
│              │  成功    │                 │  失败    │                 │    │
│              │ 返回数据 │                 │ 错误处理 │                 │    │
│              └──────────┘                 └──────────┘                 │    │
│                                                  │                     │    │
│                                                  ▼                     │    │
│                                           ┌──────────┐                 │    │
│                                           │显示通知  │                 │    │
│                                           │执行动作  │                 │    │
│                                           └──────────┘                 │    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 配置文件

### 文件位置

```
后端: src/main/resources/error-codes.yml
前端: src/config/error.config.ts (本地兜底配置)
```

### 配置格式

```yaml
# ============================================
# 成功码配置（必须有一个 success: true）
# ============================================
- code: "00000"
  message: "成功"
  level: "info"
  action: "none"
  success: true
  showNotification: false

# ============================================
# 认证相关错误 (A02xxx)
# ============================================
- code: "A0201"
  message: "用户账户不存在"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

- code: "A0231"
  message: "用户未登录"
  level: "warning"
  action: "logout"
  showNotification: true
  duration: 3

# ============================================
# 权限相关错误 (A03xxx)
# ============================================
- code: "A0301"
  message: "访问未授权"
  level: "warning"
  action: "redirect"
  redirectUrl: "/403"
  showNotification: true
  duration: 3

# ============================================
# 系统错误 (B0xxxx)
# ============================================
- code: "B0001"
  message: "系统执行出错"
  level: "error"
  action: "none"
  showNotification: true
  duration: 5
  logStackTrace: true
```

### 字段说明

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| code | string | 是 | - | 错误码，如 "A0201" |
| message | string | 是 | - | 错误消息，显示给用户 |
| level | string | 否 | "error" | 级别: info/warning/error/fatal |
| action | string | 否 | "none" | 前端动作: none/logout/redirect/retry |
| redirectUrl | string | 否 | - | 重定向URL（action=redirect时使用）|
| showNotification | boolean | 否 | true | 是否显示通知 |
| duration | integer | 否 | 3 | 通知持续时间（秒）|
| logStackTrace | boolean | 否 | false | 是否记录堆栈（error/fatal默认true）|
| success | boolean | 否 | false | 是否为成功码 |

---

## 错误码规范

### 编码规则

```
格式: [分类字母][四位数字]
示例: A0201, B0001, G0001
```

### 分类说明

| 分类 | 码段 | 场景 | 示例 |
|------|------|------|------|
| **A** | A02xxx | 认证相关 | A0201: 用户不存在, A0231: 未登录 |
| | A03xxx | 权限相关 | A0301: 未授权 |
| | A04xxx | 参数相关 | A0400: 参数错误, A0404: 路径不存在 |
| | A05xxx | 请求服务异常 | A0501: 请求次数超限 |
| **B** | B00xxx | 系统执行出错 | B0001: 系统执行出错 |
| | B01xxx | 系统超时 | B0100: 系统执行超时 |
| | B02xxx | 系统限流 | B0210: 系统限流 |
| | B03xxx | 资源异常 | B0300: 系统资源异常 |
| **C** | C00xxx | 第三方服务出错 | C0001: 调用第三方服务出错 |
| | C01xxx | 中间件服务出错 | C0130: 缓存服务出错 |
| | C03xxx | 数据库服务出错 | C0300: 数据库服务出错 |
| **G** | G00xxx | 网关路由相关 | G0001: 路由不存在, G0002: 路由已存在 |

---

## 后端实现

### 核心类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           后端核心类                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ ErrorCodeDefinition │  错误码定义实体                                   │
│   ├─────────────────────┤                                                   │
│   │ - code: String      │                                                   │
│   │ - message: String   │                                                   │
│   │ - level: String     │                                                   │
│   │ - action: String    │                                                   │
│   │ - success: Boolean  │  ← 标记成功码                                     │
│   └─────────────────────┘                                                   │
│            ▲                                                                │
│            │                                                                │
│   ┌─────────────────────┐         ┌─────────────────────┐                   │
│   │ ErrorCodeService    │◄────────│ ErrorCodeServiceImpl│                   │
│   │ (interface)         │         │                     │                   │
│   ├─────────────────────┤         │ - errorCodeMap      │                   │
│   │ + getAllErrorCodes()│         │ - successCode       │                   │
│   │ + getErrorCode()    │         ├─────────────────────┤                   │
│   │ + getSuccessCode()  │         │ + init()            │  ← @PostConstruct  │
│   │ + refresh()         │         │ + refresh()         │                   │
│   └─────────────────────┘         │ + setupSuccessCode()│                   │
│                                   └─────────────────────┘                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ Result<T>           │  统一响应结果                                     │
│   ├─────────────────────┤                                                   │
│   │ - code: String      │                                                   │
│   │ - message: String   │                                                   │
│   │ - data: T           │                                                   │
│   ├─────────────────────┤                                                   │
│   │ + ok()              │  ← 使用配置的成功码                               │
│   │ + failure()         │                                                   │
│   │ + isOk()            │  ← 判断成功码                                     │
│   │ + setSuccessCode()  │  ← 静态方法，设置成功码                           │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ GlobalExceptionHandler                                              │    │
│   ├─────────────────────┤                                                   │
│   │ + handleBizException()    处理业务异常                              │    │
│   │ + handleValidationException()  处理参数校验异常                     │    │
│   │ + handleException()       处理系统异常                              │    │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ ErrorCodeController │  配置接口                                         │
│   ├─────────────────────┤                                                   │
│   │ + getErrorCodes()   │  ← GET /api/config/error-codes                    │
│   └─────────────────────┘                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 后端加载流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        后端配置加载流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   应用启动                                                                   │
│      │                                                                      │
│      ▼                                                                      │
│   @PostConstruct                                                            │
│   ErrorCodeService.init()                                                   │
│      │                                                                      │
│      ├── 1. 加载内置默认错误码                                               │
│      │      ├── 成功码: 00000                                               │
│      │      ├── 认证错误: A02xxx                                            │
│      │      ├── 权限错误: A03xxx                                            │
│      │      ├── 参数错误: A04xxx                                            │
│      │      ├── 系统错误: B0xxxx                                            │
│      │      ├── 服务错误: C0xxxx                                            │
│      │      └── 网关错误: G0xxxx                                            │
│      │                                                                      │
│      ├── 2. 加载 error-codes.yml                                            │
│      │      ├── 文件存在? ──► 解析并覆盖默认配置                            │
│      │      └── 文件不存在? ──► 使用默认配置                                │
│      │                                                                      │
│      └── 3. 设置成功码                                                       │
│             ├── 查找标记 success=true 的配置                                │
│             ├── 找到 ──► Result.setSuccessCode(code, message)               │
│             └── 未找到 ──► 使用默认 00000                                   │
│                                                                             │
│   完成初始化，对外提供服务                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 后端使用示例

```java
// ==================== 返回成功 ====================

// 自动使用配置的成功码
return Result.ok();

// 返回数据
return Result.ok(userInfo);

// 自定义成功消息
return Result.ok("登录成功", userInfo);


// ==================== 返回错误 ====================

// 使用枚举抛出异常
throw new BizException(ErrorCodeEnum.USER_ERROR_A0231);

// 使用自定义错误码
throw new BizException("CUSTOM_001", "自定义错误消息");

// 带原始异常
throw new BizException(ErrorCodeEnum.SYSTEM_ERROR_B0001, exception);


// ==================== 判断结果 ====================

Result<User> result = userService.getUserById(id);

// 判断是否成功
if (result.isOk()) {
    User user = result.getData();
}

// 或者使用函数式风格
result.ifOk(user -> {
    // 处理成功结果
});

// 失败时抛出异常
User user = result.getOrThrow();
```

---

## 前端实现

### 核心模块图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           前端核心模块                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ types/error.ts      │  类型定义                                         │
│   ├─────────────────────┤                                                   │
│   │ ErrorCodeConfig     │  错误码配置类型                                   │
│   │ ApiError            │  API 错误类型                                     │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ config/error.config │  本地默认配置                                     │
│   ├─────────────────────┤                                                   │
│   │ defaultErrorCodes   │  兜底配置列表                                     │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ errorConfigService  │  配置服务 (核心)                                  │
│   ├─────────────────────┤                                                   │
│   │ - configMap         │  配置缓存                                         │
│   │ - successCode       │  成功码缓存                                       │
│   ├─────────────────────┤                                                   │
│   │ + init()            │  ← main.ts 调用，加载配置                         │
│   │ + getConfig()       │  获取错误码配置                                   │
│   │ + getSuccessCode()  │  获取成功码                                       │
│   │ + isSuccessCode()   │  判断是否为成功码                                 │
│   │ + refresh()         │  刷新配置                                         │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ useErrorHandler     │  错误处理 Hook                                    │
│   ├─────────────────────┤                                                   │
│   │ + handleBusinessError()   处理业务错误                              │    │
│   │ + handleHttpError()       处理 HTTP 错误                            │    │
│   │ + executeAction()         执行配置动作                              │    │
│   └─────────────────────┘                                                   │
│                                                                             │
│   ┌─────────────────────┐                                                   │
│   │ request.ts          │  请求拦截器                                       │
│   ├─────────────────────┤                                                   │
│   │ 请求拦截 ──► 添加 Token                                                 │
│   │ 响应拦截 ──► 判断成功码 ──► 错误处理                                    │
│   └─────────────────────┘                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 前端加载流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        前端配置加载流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   main.ts 应用启动                                                           │
│      │                                                                      │
│      ▼                                                                      │
│   await errorConfigService.init()                                           │
│      │                                                                      │
│      ├── 1. 加载本地默认配置                                                 │
│      │      └── defaultErrorCodes                                           │
│      │                                                                      │
│      └── 2. 请求后端配置                                                     │
│             │                                                               │
│             ├── 成功 ──► 合并后端配置                                        │
│             │           └── findSuccessCode()                               │
│             │               └── 查找标记 success=true 的配置                │
│             │                                                               │
│             └── 失败 ──► 使用本地默认配置                                    │
│                         └── 默认成功码: 00000                               │
│                                                                             │
│   完成初始化，渲染应用                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 前端请求处理流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        前端请求处理流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   发起请求                                                                   │
│      │                                                                      │
│      ▼                                                                      │
│   request.ts 拦截器                                                          │
│      │                                                                      │
│      ├── 请求拦截 ──► 添加 Token                                             │
│      │                                                                      │
│      └── 响应拦截                                                            │
│             │                                                               │
│             ├── 无 code 字段? ──► 直接返回原始数据                          │
│             │                                                               │
│             └── 有 code 字段                                                 │
│                    │                                                        │
│                    ├── 是成功码? ──► 返回 data 字段                          │
│                    │                                                        │
│                    └── 是错误码                                              │
│                         │                                                   │
│                         ▼                                                   │
│                    useErrorHandler                                          │
│                         │                                                   │
│                         ├── 查找错误码配置                                   │
│                         ├── 显示对应级别通知                                 │
│                         │   ├── info ──► message.info()                     │
│                         │   ├── warning ──► message.warning()               │
│                         │   └── error ──► message.error()                   │
│                         │                                                   │
│                         └── 执行配置动作                                     │
│                             ├── logout ──► 登出并跳转登录页                 │
│                             ├── redirect ──► 跳转指定页面                   │
│                             └── retry ──► 记录日志                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 前端使用示例

```typescript
// ==================== 请求自动处理 ====================

import request from '@/utils/request'

// 成功响应自动返回 data
const userInfo = await request({ url: '/api/user/info' })

// 错误响应自动处理
// 如返回 A0231，会自动登出并跳转登录页


// ==================== 手动错误处理 ====================

import { useErrorHandler } from '@/hooks/useErrorHandler'

const { handleBusinessError, getErrorMessage } = useErrorHandler()

// 手动处理错误
handleBusinessError({ code: 'A0231', message: '登录过期' })

// 获取错误消息（不显示通知）
const msg = getErrorMessage('A0201', '默认消息')


// ==================== 使用配置服务 ====================

import { errorConfigService } from '@/services/errorConfigService'

// 判断是否为成功码
const isSuccess = errorConfigService.isSuccessCode(response.code)

// 获取成功码
const successCode = errorConfigService.getSuccessCode()

// 获取错误码配置
const config = errorConfigService.getConfig('A0231')
if (config) {
  console.log(config.message)  // "用户未登录"
  console.log(config.action)   // "logout"
}
```

---

## 配置流程

### 修改错误码步骤

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        修改错误码步骤                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   步骤 1: 编辑配置文件                                                       │
│   ─────────────────────────────────────────────                             │
│   文件: src/main/resources/error-codes.yml                                   │
│                                                                             │
│   修改内容:                                                                  │
│   - code: "A0231"                                                           │
│     message: "您的登录已过期，请重新登录"  ← 修改这里                         │
│     level: "warning"                                                        │
│     action: "logout"                                                        │
│                                                                             │
│                                                                             │
│   步骤 2: 重启后端服务                                                       │
│   ─────────────────────────────────────────────                             │
│   重新启动 Spring Boot 应用                                                  │
│                                                                             │
│                                                                             │
│   步骤 3: 刷新前端页面                                                       │
│   ─────────────────────────────────────────────                             │
│   前端会自动拉取新的配置                                                     │
│                                                                             │
│                                                                             │
│   完成！                                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 添加新错误码步骤

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        添加新错误码步骤                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   步骤 1: 确定错误码                                                         │
│   ─────────────────────────────────────────────                             │
│   根据规范选择码段:                                                          │
│   - 认证错误: A02xxx                                                        │
│   - 权限错误: A03xxx                                                        │
│   - 参数错误: A04xxx                                                        │
│   - 系统错误: B0xxxx                                                        │
│   - 服务错误: C0xxxx                                                        │
│   - 网关错误: G0xxxx                                                        │
│                                                                             │
│   示例: CUSTOM_001 (自定义错误)                                             │
│                                                                             │
│                                                                             │
│   步骤 2: 编辑配置文件                                                       │
│   ─────────────────────────────────────────────                             │
│   在 error-codes.yml 中添加:                                                 │
│                                                                             │
│   - code: "CUSTOM_001"                                                      │
│     message: "自定义业务错误"                                                │
│     level: "warning"                                                        │
│     action: "none"                                                          │
│     showNotification: true                                                  │
│     duration: 3                                                             │
│                                                                             │
│                                                                             │
│   步骤 3: 在后端使用                                                         │
│   ─────────────────────────────────────────────                             │
│   throw new BizException("CUSTOM_001", "可选的自定义消息");                  │
│                                                                             │
│                                                                             │
│   步骤 4: 重启服务                                                           │
│   ─────────────────────────────────────────────                             │
│   重启后端服务，前端自动生效                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 修改成功码步骤

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        修改成功码步骤                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   步骤 1: 编辑配置文件                                                       │
│   ─────────────────────────────────────────────                             │
│   修改 error-codes.yml 中的成功码配置:                                       │
│                                                                             │
│   # 旧配置                                                                   │
│   - code: "00000"                                                           │
│     success: true                                                           │
│                                                                             │
│   # 新配置                                                                   │
│   - code: "200"           ← 修改成功码                                       │
│     message: "操作成功"                                                      │
│     level: "info"                                                           │
│     success: true         ← 必须标记为 true                                  │
│     showNotification: false                                                 │
│                                                                             │
│   注意: 只能有一个 success: true 的配置                                      │
│                                                                             │
│                                                                             │
│   步骤 2: 重启后端服务                                                       │
│   ─────────────────────────────────────────────                             │
│   重启后 Result.ok() 会自动使用新的成功码                                    │
│                                                                             │
│                                                                             │
│   步骤 3: 前端自动适配                                                       │
│   ─────────────────────────────────────────────                             │
│   前端会自动从后端获取新的成功码配置                                         │
│   无需修改前端代码                                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 使用示例

### 完整配置示例

```yaml
# error-codes.yml

# ============================================
# 成功码配置
# ============================================
- code: "00000"
  message: "操作成功"
  level: "info"
  action: "none"
  success: true
  showNotification: false

# ============================================
# 认证相关错误
# ============================================
- code: "A0201"
  message: "用户账户不存在"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

- code: "A0210"
  message: "密码错误"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

- code: "A0230"
  message: "登录已过期，请重新登录"
  level: "warning"
  action: "logout"
  showNotification: true
  duration: 3

- code: "A0231"
  message: "用户未登录"
  level: "warning"
  action: "logout"
  showNotification: true
  duration: 3

# ============================================
# 权限相关错误
# ============================================
- code: "A0301"
  message: "访问未授权"
  level: "warning"
  action: "redirect"
  redirectUrl: "/403"
  showNotification: true
  duration: 3

# ============================================
# 参数相关错误
# ============================================
- code: "A0400"
  message: "请求参数错误"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

- code: "A0404"
  message: "请求的资源不存在"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

# ============================================
# 系统错误
# ============================================
- code: "B0001"
  message: "系统繁忙，请稍后重试"
  level: "error"
  action: "none"
  showNotification: true
  duration: 5
  logStackTrace: true

- code: "B0210"
  message: "请求过于频繁，请稍后再试"
  level: "warning"
  action: "retry"
  showNotification: true
  duration: 3

# ============================================
# 网关错误
# ============================================
- code: "G0001"
  message: "路由不存在"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

- code: "G0002"
  message: "路由已存在"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3

# ============================================
# 自定义错误码
# ============================================
- code: "CUSTOM_001"
  message: "业务规则校验失败"
  level: "warning"
  action: "none"
  showNotification: true
  duration: 3
```

### 后端 Controller 示例

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/info")
    public Result<UserInfo> getUserInfo() {
        // 自动使用配置的成功码
        return Result.ok(userService.getCurrentUser());
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        try {
            LoginResult result = authService.login(request);
            return Result.ok("登录成功", result);
        } catch (UserNotFoundException e) {
            // 使用配置的错误码
            throw new BizException("A0201");
        } catch (PasswordErrorException e) {
            throw new BizException("A0210");
        }
    }

    @GetMapping("/admin-only")
    public Result<Void> adminOnly() {
        if (!currentUser.isAdmin()) {
            // 权限错误
            throw new BizException(ErrorCodeEnum.USER_ERROR_A0301);
        }
        return Result.ok();
    }
}
```

### 前端页面示例

```vue
<template>
  <div>
    <a-button @click="fetchUserInfo">获取用户信息</a-button>
    <a-button @click="login">登录</a-button>
  </div>
</template>

<script setup lang="ts">
import request from '@/utils/request'
import { message } from 'ant-design-vue'

// 请求自动处理成功和错误
async function fetchUserInfo() {
  try {
    // 成功时自动返回 data
    const userInfo = await request({ url: '/api/user/info' })
    console.log(userInfo)
  } catch (error) {
    // 错误已自动处理，无需额外操作
    console.log('请求失败')
  }
}

async function login() {
  try {
    const result = await request({
      url: '/api/user/login',
      method: 'post',
      data: { username: 'admin', password: '123456' }
    })
    message.success('登录成功')
  } catch (error) {
    // 密码错误时会自动显示 "密码错误" 通知
    // 登录过期时会自动登出并跳转登录页
  }
}
</script>
```

---

## 注意事项

### 1. 必须配置成功码

```yaml
# 必须有一个标记 success: true 的配置
- code: "00000"
  message: "成功"
  success: true  # ← 必须
```

如果没有配置，系统会使用默认的 `"00000"`。

### 2. 错误码唯一性

YAML 配置中，相同的 `code` 会被后加载的配置覆盖。

```yaml
# 先加载
- code: "A0201"
  message: "用户不存在"

# 后加载（会覆盖上面的配置）
- code: "A0201"
  message: "该用户不存在"
```

### 3. YAML 格式要求

```yaml
# 正确
- code: "A0201"
  message: "用户不存在"

# 错误（缺少空格）
- code:"A0201"
  message:"用户不存在"
```

### 4. 重启生效

修改 `error-codes.yml` 后，**必须重启后端服务**才能生效。

### 5. 前端缓存

前端在应用启动时拉取配置并缓存，如需刷新配置，需要刷新页面。

### 6. 兜底机制

- 后端：YAML 加载失败时，使用内置默认配置
- 前端：后端请求失败时，使用本地默认配置
- 成功码：未配置时，使用默认 `"00000"`

---

## 文件清单

### 后端文件

| 文件路径 | 说明 |
|---------|------|
| `common/exception/ErrorCodeDefinition.java` | 错误码定义实体 |
| `service/ErrorCodeService.java` | 错误码服务接口 |
| `service/impl/ErrorCodeServiceImpl.java` | 错误码服务实现 |
| `common/web/Result.java` | 统一响应结果 |
| `common/web/GlobalExceptionHandler.java` | 全局异常处理器 |
| `controller/ErrorCodeController.java` | 错误码配置接口 |
| `resources/error-codes.yml` | 错误码配置文件 |

### 前端文件

| 文件路径 | 说明 |
|---------|------|
| `types/error.ts` | 错误类型定义 |
| `config/error.config.ts` | 本地默认配置 |
| `services/errorConfigService.ts` | 错误码配置服务 |
| `hooks/useErrorHandler.ts` | 错误处理 Hook |
| `utils/request.ts` | 请求拦截器 |
| `main.ts` | 应用入口（初始化配置）|

---

**文档版本**: 1.0  
**最后更新**: 2026-03-04  
**作者**: AI Architect
