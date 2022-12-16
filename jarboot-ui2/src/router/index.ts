import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/home.vue';
import Services from '@/views/services/services.vue';
import login from '@/views/login.vue';
import OAuthService from "@/services/OAuthService";
import StringUtil from "@/common/StringUtil";

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: login
    },
    {
      path: '/',
      component: Home,
      children: [
        {
          path: '',
          component: Services
        },
        {
          path: '/diagnose',
          name: 'diagnose',
          component: Services
        },
        {
          path: '/authority',
          name: 'authority',
          component: Services
        },
        {
          path: '/setting',
          name: 'setting',
          component: Services
        },
      ]
    },
  ]
});

router.beforeEach(async (to, from, next) => {
  const toPath = to.path;
  if ('/login' === toPath) {
    next();
    return;
  }
  const resp = await OAuthService.getCurrentUser();
  console.info('login resp:', resp);
  if (StringUtil.isEmpty(resp?.result?.username)) {
    next({ path: '/login', force: true });
    return;
  }
  next();
  // 权限检验
});

export default router
