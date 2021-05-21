import { defineConfig } from 'umi';

export default defineConfig({
  nodeModulesTransform: {
    type: 'none',
  },
  routes: [
    { path: '/', component: '@/pages/index' },
  ],
  fastRefresh: {},
  proxy: {
    '/jarboot-service': {
      'target': 'http://localhost:9899/jarboot-service/',
      'changeOrigin': true,
      'pathRewrite': {'^/jarboot-service': ''}
    }
  },
});
