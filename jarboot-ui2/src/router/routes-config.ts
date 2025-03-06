import {
  FILE_MGR,
  PAGE_COMMON,
  PAGE_JVM,
  PAGE_MONITOR,
  PAGE_PREFERENCES,
  PAGE_PRIVILEGE,
  PAGE_ROLE,
  PAGE_SERVICE,
  PAGE_SETTING,
  PAGE_TRUST_HOSTS,
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
      module: 'SERVICES_MGR',
      code: 'SERVICES_MGR',
      icon: 'HomeFilled',
    },
  },
  {
    path: '/jvm-diagnose',
    name: PAGE_JVM,
    component: () => import('@/views/services/service-manager.vue'),
    meta: {
      keepAlive: true,
      module: 'ONLINE_DEBUG',
      code: 'ONLINE_DEBUG',
      icon: 'icon-debug',
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
      icon: 'icon-terminal',
    },
    children: [
      {
        path: '/monitor',
        name: PAGE_MONITOR,
        component: () => import('@/views/tools/monitor.vue'),
        meta: {
          keepAlive: true,
          module: 'TOOLS',
          code: 'MONITOR',
          icon: 'icon-monitor',
        },
      },
      {
        path: 'file-manager',
        name: FILE_MGR,
        component: () => import('@/views/tools/file-browse.vue'),
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
      icon: 'Setting',
    },
    children: [
      {
        path: 'preference',
        name: PAGE_PREFERENCES,
        component: () => import('@/views/setting/preferences-config.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          icon: 'icon-preferences',
          code: 'PREFERENCES_CONFIG',
        },
      },
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
      {
        path: 'trusted-hosts',
        name: PAGE_TRUST_HOSTS,
        component: () => import('@/views/setting/trusted-hosts.vue'),
        meta: {
          keepAlive: true,
          module: 'SETTING',
          icon: 'Memo',
          code: 'TRUSTED_HOSTS',
        },
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/setting/audit-log.vue'),
        meta: {
          module: 'SETTING',
          icon: 'Notebook',
          code: '日志记录',
        },
      },
    ],
  },
];
