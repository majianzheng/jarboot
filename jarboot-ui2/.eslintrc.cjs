/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,
  'extends': [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier'
  ],
  parserOptions: {
    ecmaVersion: 'latest'
  },
  rules: {
    'no-prototype-builtins': 0,
    'vue/no-deprecated-v-on-native-modifier': 0,
    'vue/multi-word-component-names': 0,
    'vue/no-parsing-error': 0,
    'vue/no-mutating-props': 0,
  }
}
