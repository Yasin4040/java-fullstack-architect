# Vben Admin 5.0 迁移快速参考

## 📋 快速开始（5 分钟）

```bash
# 1. 克隆项目
git clone https://github.com/vbenjs/vue-vben-admin.git lite-gateway-admin-vben
cd lite-gateway-admin-vben

# 2. 安装依赖
corepack enable
pnpm install

# 3. 启动项目
cd apps/web-antd
pnpm dev
```

---

## 📁 需要复制的核心文件

```bash
# 从原项目复制到新项目
lite-gateway-ui/src/api/route.ts       → apps/web-antd/src/api/gateway/route.ts
lite-gateway-ui/src/api/rateLimit.ts   → apps/web-antd/src/api/gateway/rateLimit.ts
lite-gateway-ui/src/api/log.ts         → apps/web-antd/src/api/gateway/log.ts
lite-gateway-ui/src/api/user.ts        → apps/web-antd/src/api/gateway/user.ts
lite-gateway-ui/src/types/api.ts       → apps/web-antd/src/types/api.ts
lite-gateway-ui/src/utils/request.ts   → apps/web-antd/src/utils/request.ts
```

---

## ⚙️ 关键配置

### 1. 环境变量配置（.env.development）

```bash
VITE_GLOB_API_URL=/api
VITE_GLOB_API_URL_PREFIX=/api
VITE_GLOB_API_DEV_URL=http://localhost:8080
```

### 2. Vite 代理配置（vite.config.mts）

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

### 3. 请求拦截器（src/utils/request.ts）

```typescript
import { RequestClient } from '@vben/request';

const request = new RequestClient({
  baseURL: import.meta.env.VITE_GLOB_API_URL || '/api',
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data.code !== undefined) {
      if (['00000', '0', '200'].includes(String(data.code))) {
        return data.data;
      }
      throw new Error(data.message || '请求失败');
    }
    return data;
  },
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default request;
```

---

## 🎯 页面创建模板

### 路由管理页面（src/views/gateway/route/index.vue）

```vue
<template>
  <BasicTable @register="registerTable">
    <template #toolbar>
      <a-button type="primary" @click="handleAdd">新增路由</a-button>
    </template>
  </BasicTable>
</template>

<script lang="ts" setup>
import { BasicTable, useTable } from '@vben/common-ui';
import { routeApi } from '../../api/gateway/route';

const [registerTable] = useTable({
  api: getRoutePage,
  columns: [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '路由名称', dataIndex: 'name', width: 150 },
    { title: '目标URI', dataIndex: 'uri', ellipsis: true },
  ],
  useSearchForm: true,
});

async function getRoutePage(params: any) {
  const result = await routeApi.getRoutePage(params);
  return { items: result.list, total: result.total };
}

function handleAdd() {
  console.log('新增路由');
}
</script>
```

---

## 🛣️ 路由配置模板（src/router/routes/modules/gateway.ts）

```typescript
import type { AppRouteModule } from '@vben/types';

const gateway: AppRouteModule = {
  path: '/gateway',
  name: 'Gateway',
  component: 'LAYOUT',
  redirect: '/gateway/route',
  meta: {
    icon: 'carbon:network-2',
    orderNo: 1000,
    title: '网关管理',
  },
  children: [
    {
      path: 'route',
      name: 'RouteManagement',
      component: () => import('@/views/gateway/route/index.vue'),
      meta: { title: '路由管理' },
    },
  ],
};

export default gateway;
```

---

## 🚀 常用命令

```bash
# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 构建生产版本
pnpm build

# 预览生产版本
pnpm preview

# 类型检查
pnpm type-check

# 代码检查
pnpm lint

# 代码格式化
pnpm format
```

---

## 🔧 常见问题速查

### 安装依赖失败
```bash
pnpm config set registry https://registry.npmmirror.com
pnpm store prune
pnpm install
```

### 页面空白
1. 检查浏览器控制台错误
2. 确认后端服务已启动
3. 检查 API 代理配置

### API 请求 404
1. 确认后端服务运行在 8080 端口
2. 检查 vite.config.mts 代理配置
3. 检查 .env.development API 地址

### 类型错误
```bash
pnpm type-check
```

---

## 📚 参考链接

- **官方文档**：https://doc.vben.pro/
- **GitHub**：https://github.com/vbenjs/vue-vben-admin
- **Ant Design Vue**：https://antdv.com/
- **Vue 3**：https://cn.vuejs.org/
- **Vite**：https://cn.vitejs.dev/

---

## 💡 迁移检查清单

- [ ] 克隆 Vben Admin 项目
- [ ] 安装项目依赖
- [ ] 配置环境变量
- [ ] 配置 Vite 代理
- [ ] 创建请求拦截器
- [ ] 复制 API 文件
- [ ] 复制类型定义
- [ ] 创建业务页面
- [ ] 配置路由
- [ ] 启动项目测试
- [ ] 对接后端 API
- [ ] 功能测试

---

**提示**：详细步骤请参考 [VbenAdmin迁移教程.md](./VbenAdmin迁移教程.md)
