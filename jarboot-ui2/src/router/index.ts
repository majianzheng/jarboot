import { createRouter, createWebHashHistory } from 'vue-router';
import Home from '../views/home.vue';
import Services from '@/views/services/services.vue';
import Setting from '@/views/setting/setting.vue';
import CommonSetting from '@/views/setting/common-setting.vue';
import UserManager from '@/views/setting/user-manager.vue';
import login from '@/views/login.vue';
import OAuthService from '@/services/OAuthService';
import StringUtil from '@/common/StringUtil';
import { useUserStore } from '@/stores';

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: login,
    },
    {
      path: '/',
      component: Home,
      children: [
        {
          path: '',
          component: Services,
          meta: {
            keepAlive: true,
          },
        },
        {
          path: '/setting',
          name: 'setting',
          component: Setting,
          children: [
            {
              path: 'common',
              component: CommonSetting,
              meta: {
                keepAlive: true,
              },
            },
            {
              path: 'user',
              name: 'user',
              component: UserManager,
              meta: {
                keepAlive: true,
              },
            },
          ],
        },
      ],
    },
  ],
});

router.beforeEach(async (to, from, next) => {
  const toPath = to.path;
  if ('/login' === toPath) {
    next();
    return;
  }
  const user = (await OAuthService.getCurrentUser()) as any;
  if (StringUtil.isEmpty(user?.username)) {
    next({ path: '/login', force: true });
    return;
  }
  const userStore = useUserStore();
  userStore.setCurrentUser(user);
  next();
  // 权限检验
});

export default router;
