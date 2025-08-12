import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import IconLoading from '@/components/icon-loading.vue';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import '@/styles/index.scss';
import 'uno.css';
import { createI18n } from 'vue-i18n';

import App from './App.vue';
import router from './router';
import zh from './locales/zh-CN';
import zhTw from './locales/zh-TW';
import en from './locales/en-US';
import './assets/iconfont/iconfont.css';
import './assets/iconfont/iconfont.js';
import './assets/main.less';

const i18n = createI18n({
  globalInjection: true,
  locale: localStorage.getItem('locale') ?? 'zh-CN',
  legacy: false,
  messages: {
    'zh-CN': zh, // 中文语言包
    'zh-TW': zhTw, // 中文繁体语言包
    'en-US': en, // 英文语言包
  },
});

const app = createApp(App, { i18n });

app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.use(i18n);
app.component('IconLoading', IconLoading);
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.mount('#app');
