import { defineConfig } from 'umi';

export default defineConfig({
  layout: false,
  devtool: 'source-map',
  locale: {
    default: 'zh-CN',
    antd: true,
    baseNavigator: true,
  },

  //图片文件是否走 base64 编译的阈值
  inlineLimit: 1000000,
});
