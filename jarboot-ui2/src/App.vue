<script setup lang="ts">
import { RouterView } from 'vue-router';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import zhTw from 'element-plus/dist/locale/zh-tw.mjs';
import en from 'element-plus/dist/locale/en.mjs';
import { useI18n } from 'vue-i18n';
import { computed, onMounted, onUnmounted } from 'vue';
import { useBasicStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';

const { locale } = useI18n();
const locales = { 'zh-CN': zhCn, 'zh-TW': zhTw, 'en-US': en } as any;
const language = computed(() => locales[locale.value]);
const basic = useBasicStore();
onMounted(() => {
  window.onresize = () => {
    basic.update();
  };
  CommonUtils.init();
});
onUnmounted(() => {
  window.onresize = null;
});
</script>

<template>
  <el-config-provider :locale="language">
    <RouterView />
  </el-config-provider>
</template>

<style lang="less" scoped></style>
