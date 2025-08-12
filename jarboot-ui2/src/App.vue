<script setup lang="ts">
import { RouterView } from 'vue-router';
import { zhCn, zhTw, en } from 'element-plus/es/locale/index';
import { type I18n, useI18n } from 'vue-i18n';
import { computed, onMounted, onUnmounted } from 'vue';
import { useBasicStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';

const { locale } = useI18n();
const locales = { 'zh-CN': zhCn, 'zh-TW': zhTw, 'en-US': en } as any;
const language = computed(() => locales[locale.value]);
const basic = useBasicStore();
const props = defineProps<{
  i18n: I18n;
}>();

onMounted(async () => {
  await basic.init();
  window.onresize = () => {
    basic.update();
  };
  document.onkeydown = event => {
    if (event.ctrlKey || event.metaKey) {
      if ('KeyF' === event.code || 'KeyS' === event.code) {
        return false;
      }
    }
  };
  CommonUtils.init(props.i18n);
});
onUnmounted(() => {
  window.onresize = null;
});
</script>

<template>
  <el-config-provider :locale="language">
    <router-view></router-view>
  </el-config-provider>
</template>
