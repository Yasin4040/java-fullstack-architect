import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/dashboard/index.vue'),
    meta: {
      icon: 'DashboardOutlined',
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
  },
  {
    component: () => import('#/views/route/index.vue'),
    meta: {
      icon: 'NodeIndexOutlined',
      title: $t('page.route.title'),
    },
    name: 'Route',
    path: '/route',
  },
  {
    component: () => import('#/views/rate-limit/index.vue'),
    meta: {
      icon: 'SafetyOutlined',
      title: $t('page.rateLimit.title'),
    },
    name: 'RateLimit',
    path: '/rate-limit',
  },
  {
    component: () => import('#/views/log/index.vue'),
    meta: {
      icon: 'FileTextOutlined',
      title: $t('page.log.title'),
    },
    name: 'Log',
    path: '/log',
  },
  {
    meta: {
      icon: 'SettingOutlined',
      title: $t('page.system.title'),
    },
    name: 'System',
    path: '/system',
    children: [
      {
        component: () => import('#/views/system/user.vue'),
        meta: {
          icon: 'UserOutlined',
          title: $t('page.system.user.title'),
        },
        name: 'User',
        path: 'user',
      },
    ],
  },
];

export default routes;
