import path from 'path';
import { fileURLToPath } from 'node:url';
/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';

import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons';

import Unocss from 'unocss/vite';
import { presetAttributify, presetIcons, presetUno, transformerDirectives, transformerVariantGroup } from 'unocss';

const host = 'http://localhost:9899';
const rootDir = path.dirname(fileURLToPath(import.meta.url));
// https://vitejs.dev/config/
export default defineConfig({
  base: '/jarboot/',
  resolve: {
    alias: {
      '@': path.join(rootDir, './src'),
    },
  },
  server: {
    proxy: {
      '/api': host,
      '/jarboot/plugins': host,
      '/jarboot/preferences': host,
    },
  },
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler', silenceDeprecations: ['legacy-js-api'] },
    },
  },
  optimizeDeps: {
    include: [
      'element-plus/es',
      '@vueuse/core',
      'echarts',
      'codemirror',
      '@xterm/xterm',
      '@xterm/addon-attach',
      '@xterm/addon-canvas',
      '@xterm/addon-fit',
      '@xterm/addon-search',
      '@xterm/addon-serialize',
      '@xterm/addon-unicode11',
      '@xterm/addon-web-links',
    ],
  },
  plugins: [
    vue(),
    vueJsx(),
    Components({
      extensions: ['vue', 'md'],
      // allow auto import and register components used in markdown
      include: [/\.vue$/, /\.vue\?vue/, /\.ts$/, /\.md$/],
      resolvers: [
        ElementPlusResolver({
          importStyle: false,
        }),
      ],
      dts: 'src/components.d.ts',
    }),

    Unocss({
      presets: [
        presetUno(),
        presetAttributify(),
        presetIcons({
          scale: 1.2,
          warn: true,
        }),
      ],
      transformers: [transformerDirectives(), transformerVariantGroup()],
    }),
    createSvgIconsPlugin({
      iconDirs: [path.resolve(rootDir, 'src/assets')],
      symbolId: 'icon-[dir]-[name]',
      customDomId: '__svg__icons__dom__',
    }),
  ],
  test: {
    server: {
      deps: {
        inline: ['element-plus'],
      },
    },
  },
});
