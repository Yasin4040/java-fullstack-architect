import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:dashboard',
      keepAlive: true,
      order: 100,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
    component: () => import('#/views/dashboard/index.vue'),
  },
  {
    meta: {
      icon: 'lucide:route',
      keepAlive: true,
      order: 200,
      title: $t('page.route.title'),
    },
    name: 'Route',
    path: '/route',
    component: () => import('#/views/route/index.vue'),
  },
  {
    meta: {
      icon: 'lucide:shield',
      keepAlive: true,
      order: 300,
      title: $t('page.rateLimit.title'),
    },
    name: 'RateLimit',
    path: '/rate-limit',
    component: () => import('#/views/rate-limit/index.vue'),
  },
  {
    meta: {
      icon: 'lucide:log',
      keepAlive: true,
      order: 400,
      title: $t('page.log.title'),
    },
    name: 'Log',
    path: '/log',
    component: () => import('#/views/log/index.vue'),
  },
  {
    meta: {
      icon: 'lucide:users',
      keepAlive: true,
      order: 500,
      title: $t('page.system.user'),
    },
    name: 'SystemUser',
    path: '/system/user',
    component: () => import('#/views/system/user.vue'),
  },
];

export default routes;