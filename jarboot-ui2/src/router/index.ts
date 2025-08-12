import { createRouter, createWebHashHistory } from 'vue-router';
import OAuthService from '@/services/OAuthService';
import StringUtil from '@/common/StringUtil';
import { useUserStore } from '@/stores';
import { PAGE_LOGIN } from '@/common/route-name-constants';
import routesConfig from '@/router/routes-config';
import CommonNotice from '@/common/CommonNotice';

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: PAGE_LOGIN,
      component: () => import('@/views/login.vue'),
    },
    {
      path: '/win-terminal',
      name: 'win-terminal',
      component: () => import('@/views/win-terminal.vue'),
      meta: {
        keepAlive: true,
        roles: ['ROLE_ADMIN'],
      },
    },
    {
      path: '/',
      component: () => import('@/views/home.vue'),
      children: routesConfig,
    },
  ],
});

router.beforeEach(async (to, _from, next) => {
  if (PAGE_LOGIN === to.name) {
    next();
    return;
  }
  const user = await OAuthService.getCurrentUser();
  if (StringUtil.isEmpty(user?.username)) {
    next({ name: PAGE_LOGIN, force: true });
    return;
  }
  const userStore = useUserStore();
  userStore.setCurrentUser(user);
  if (null === userStore.avatar) {
    await userStore.fetchAvatar();
  }
  if ('/' === to.path) {
    next();
    return;
  }
  if ('jarboot' === userStore.username) {
    // jarboot用户无需校验
    next();
    return;
  }
  const code = to?.meta?.code as string;
  if (code) {
    if (!userStore.privileges[code]) {
      // 无权限
      CommonNotice.warn('无权限');
      return;
    }
  }
  next();
});

export default router;
