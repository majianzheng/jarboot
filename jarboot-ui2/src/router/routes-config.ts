import { PAGE_COMMON, PAGE_JVM, PAGE_SERVICE, PAGE_SETTING, PAGE_USER } from '@/common/route-name-constants';

export default [
  {
    path: '/services',
    name: PAGE_SERVICE,
    component: () => import('@/views/services/service-manager.vue'),
    meta: {
      keepAlive: true,
      menu: true,
      module: 'SERVICES_MGR',
      code: 'SERVICES_MGR',
    },
  },
  {
    path: '/jvm-diagnose',
    name: PAGE_JVM,
    component: () => import('@/views/services/service-manager.vue'),
    meta: {
      keepAlive: true,
      menu: true,
      module: 'ONLINE_DEBUG',
      code: 'ONLINE_DEBUG',
    },
  },
  {
    path: '/terminal',
    name: 'terminal',
    component: () => import('@/views/tools/tools-main.vue'),
    meta: {
      keepAlive: true,
      menu: true,
      module: 'TERMINAL',
    },
  },
  {
    path: '/setting',
    name: PAGE_SETTING,
    component: () => import('@/views/setting/setting.vue'),
    meta: {
      keepAlive: true,
      menu: true,
      module: 'SETTING',
      code: 'SETTING',
    },
    children: [
      {
        path: 'common',
        name: PAGE_COMMON,
        component: () => import('@/views/setting/common-setting.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          code: 'SYSTEM_SETTING',
        },
      },
      {
        path: 'user',
        name: PAGE_USER,
        component: () => import('@/views/setting/user-manager.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          code: 'USER_LIST',
        },
      },
    ],
  },
];
