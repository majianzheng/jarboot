import {
  FILE_MGR,
  PAGE_COMMON,
  PAGE_JVM,
  PAGE_PRIVILEGE,
  PAGE_ROLE,
  PAGE_SERVICE,
  PAGE_SETTING,
  PAGE_USER,
  TERM,
  TOOLS,
} from '@/common/route-name-constants';

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
    path: '/tools',
    name: TOOLS,
    component: () => import('@/views/tools/tools-main.vue'),
    meta: {
      keepAlive: true,
      menu: true,
      module: 'TOOLS',
      code: 'TOOLS',
    },
    children: [
      {
        path: 'file-manager',
        name: FILE_MGR,
        component: () => import('@/views/tools/file-cloud.vue'),
        meta: {
          keepAlive: true,
          module: 'TOOLS',
          icon: 'icon-file-manager',
          code: 'FILE_MGR',
        },
      },
      {
        path: 'terminal',
        name: TERM,
        component: () => import('@/views/tools/web-terminal.vue'),
        meta: {
          keepAlive: true,
          module: 'TOOLS',
          icon: 'icon-terminal',
          code: 'TERMINAL',
        },
      },
    ],
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
          icon: 'Setting',
          code: 'SYSTEM_SETTING',
        },
      },
      {
        path: 'role',
        name: PAGE_ROLE,
        component: () => import('@/views/setting/role-manager.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          icon: 'icon-role',
          code: 'ROLE_MGR',
        },
      },
      {
        path: 'user',
        name: PAGE_USER,
        component: () => import('@/views/setting/user-manager.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          icon: 'UserFilled',
          code: 'USER_LIST',
        },
      },
      {
        path: 'privilege',
        name: PAGE_PRIVILEGE,
        component: () => import('@/views/setting/privilege-manager.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          icon: 'icon-privilege',
          code: 'PRIVILEGE_MGR',
        },
      },
    ],
  },
];
