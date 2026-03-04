# Lite Gateway UI

Lite Gateway 管理后台前端项目，基于 Vue3 + TypeScript + Pinia + Axios + Ant Design Vue 技术栈构建。

## 技术栈

- **Vue 3.4+** - 渐进式 JavaScript 框架
- **TypeScript 5.0+** - 类型安全的 JavaScript 超集
- **Vue Router 4** - Vue.js 官方路由管理器
- **Pinia 2.1+** - Vue 官方状态管理库
- **Axios** - 基于 Promise 的 HTTP 客户端
- **Ant Design Vue 4** - 企业级 UI 组件库
- **Vite 5** - 下一代前端构建工具
- **ECharts** - 数据可视化图表库
- **Day.js** - 轻量级日期处理库

## 功能特性

- 路由管理 - 动态路由配置、路由状态管理、配置刷新
- 限流配置 - 基于令牌桶算法的限流规则管理
- 日志监控 - 访问日志查询、统计分析、图表展示
- 用户管理 - 系统用户管理、权限控制
- 仪表盘 - 数据可视化、实时监控

## 项目结构

```
lite-gateway-ui/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口封装
│   │   ├── route.ts       # 路由管理接口
│   │   ├── rateLimit.ts   # 限流规则接口
│   │   ├── log.ts         # 日志管理接口
│   │   └── user.ts        # 用户管理接口
│   ├── components/        # 公共组件
│   ├── layouts/           # 布局组件
│   │   ├── MainLayout.vue # 主布局
│   │   └── components/    # 布局子组件
│   ├── router/            # 路由配置
│   │   └── index.ts       # 路由定义
│   ├── store/             # Pinia 状态管理
│   │   ├── modules/       # 状态模块
│   │   │   ├── user.ts    # 用户状态
│   │   │   ├── route.ts   # 路由状态
│   │   │   ├── rateLimit.ts # 限流状态
│   │   │   └── log.ts     # 日志状态
│   │   └── index.ts       # 状态入口
│   ├── types/             # TypeScript 类型定义
│   │   └── api.ts         # API 类型定义
│   ├── utils/             # 工具函数
│   │   └── request.ts     # Axios 封装
│   ├── views/             # 页面视图
│   │   ├── login/         # 登录页
│   │   ├── dashboard/     # 仪表盘
│   │   ├── route/         # 路由管理
│   │   ├── rate-limit/    # 限流配置
│   │   ├── log/           # 日志监控
│   │   ├── system/        # 系统管理
│   │   └── error/         # 错误页面
│   ├── App.vue            # 根组件
│   ├── main.ts            # 入口文件
│   └── vite-env.d.ts      # Vite 类型声明
├── index.html             # HTML 模板
├── package.json           # 项目依赖
├── tsconfig.json          # TypeScript 配置
├── tsconfig.node.json     # Node 类型配置
└── vite.config.ts         # Vite 配置
```

## 快速开始

### 环境要求

- Node.js 18+
- npm 9+ 或 yarn 1.22+

### 安装依赖

```bash
cd lite-gateway-ui
npm install
```

### 开发环境运行

```bash
npm run dev
```

默认访问地址：http://localhost:3000

### 生产环境构建

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

### 预览生产构建

```bash
npm run preview
```

## 后端 API 配置

在 `vite.config.ts` 中配置代理：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端服务地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

生产环境请在 nginx 或服务器中配置反向代理。

## 主要功能模块

### 1. 路由管理

- 路由列表查询（分页、筛选）
- 新增/编辑/删除路由
- 路由状态启用/禁用
- 刷新网关配置
- 路由详情查看

### 2. 限流配置

- 限流规则列表
- 令牌桶参数配置（每秒补充速率、桶容量）
- 限流类型（IP限流、用户限流、全局限流）
- 规则状态管理

### 3. 日志监控

- 访问日志查询
- 多维度筛选（路由ID、路径、方法、状态码、时间范围）
- 统计概览（总请求数、成功率、平均响应时间）
- 日志详情查看

### 4. 用户管理

- 用户列表查询
- 新增/编辑/删除用户
- 密码重置
- 用户状态管理

## 开发规范

### 代码风格

- 使用 TypeScript 严格模式
- 组件使用 Composition API + `<script setup>` 语法
- 使用 Pinia 进行状态管理
- API 接口统一封装在 `api/` 目录

### 命名规范

- 组件名：PascalCase（如 `UserTable.vue`）
- 文件名：kebab-case（如 `rate-limit.ts`）
- 变量/函数：camelCase
- 常量：UPPER_SNAKE_CASE
- 类型/接口：PascalCase + 后缀（如 `UserQuery`）

### Git 提交规范

```
feat: 新功能
fix: 修复问题
docs: 文档修改
style: 代码格式修改（不影响代码运行的变动）
refactor: 重构
perf: 性能优化
test: 测试用例修改
chore: 构建过程或辅助工具的变动
```

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 开源协议

[MIT License](LICENSE)

## 相关项目

- [Lite Gateway Core](../lite-gateway-core) - 网关核心服务
- [Lite Gateway Admin](../lite-gateway-admin) - 网关管理后台服务

## 联系我们

如有问题或建议，欢迎提交 Issue 或 Pull Request。
