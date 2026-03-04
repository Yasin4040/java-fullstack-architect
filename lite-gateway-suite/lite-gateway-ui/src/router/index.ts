import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'DashboardOutlined' }
      },
      {
        path: 'route',
        name: 'Route',
        component: () => import('@/views/route/index.vue'),
        meta: { title: '路由管理', icon: 'NodeIndexOutlined' }
      },
      {
        path: 'rate-limit',
        name: 'RateLimit',
        component: () => import('@/views/rate-limit/index.vue'),
        meta: { title: '限流配置', icon: 'SafetyOutlined' }
      },
      {
        path: 'log',
        name: 'Log',
        component: () => import('@/views/log/index.vue'),
        meta: { title: '日志监控', icon: 'FileTextOutlined' }
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统管理', icon: 'SettingOutlined' },
        children: [
          {
            path: 'user',
            name: 'User',
            component: () => import('@/views/system/user.vue'),
            meta: { title: '用户管理', icon: 'UserOutlined' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.public) {
    next()
    return
  }
  
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  // 如果已登录但没有用户信息，获取用户信息
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      next('/login')
      return
    }
  }
  
  next()
})

export default router
