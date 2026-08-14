import js from '@eslint/js';
import pluginVue from 'eslint-plugin-vue';
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript';
import configPrettier from '@vue/eslint-config-prettier';

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },
  {
    name: 'app/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/dist-ssr/**',
      '**/coverage/**',
      '**/node_modules/**',
      'src/components.d.ts',
      'src/assets/**',
      'public/**',
      '**/StringUtil.ts',
      '**/j-console.vue',
    ],
  },
  js.configs.recommended,
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  configPrettier,
  {
    rules: {
      'no-prototype-builtins': 0,
      'vue/no-deprecated-v-on-native-modifier': 0,
      'vue/multi-word-component-names': 0,
      'vue/no-parsing-error': 0,
      'vue/no-mutating-props': 0,
      'vue/block-lang': 0,
      '@typescript-eslint/no-explicit-any': 0,
      '@typescript-eslint/ban-ts-comment': 0,
      '@typescript-eslint/no-unused-expressions': ['error', { allowShortCircuit: true, allowTernary: true }],
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    },
  }
);