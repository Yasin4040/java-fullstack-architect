import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/dashboard/index.vue'),
    meta: {
      icon: 'DashboardOutlined',
      keepAlive: true,
      order: 100,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
  },
  {
    component: () => import('#/views/route/index.vue'),
    meta: {
      icon: 'NodeIndexOutlined',
      keepAlive: true,
      order: 200,
      title: $t('page.route.title'),
    },
    name: 'Route',
    path: '/route',
  },
  {
    component: () => import('#/views/rate-limit/index.vue'),
    meta: {
      icon: 'SafetyOutlined',
      keepAlive: true,
      order: 300,
      title: $t('page.rateLimit.title'),
    },
    name: 'RateLimit',
    path: '/rate-limit',
  },
  {
    component: () => import('#/views/log/index.vue'),
    meta: {
      icon: 'FileTextOutlined',
      keepAlive: true,
      order: 400,
      title: $t('page.log.title'),
    },
    name: 'Log',
    path: '/log',
  },
  {
    meta: {
      icon: 'SettingOutlined',
      order: 500,
      title: $t('page.system.title'),
    },
    name: 'System',
    path: '/system',
    children: [
      {
        component: () => import('#/views/system/user.vue'),
        meta: {
          icon: 'UserOutlined',
          keepAlive: true,
          title: $t('page.system.user.title'),
        },
        name: 'User',
        path: 'user',
      },
    ],
  },
];

export default routes;
