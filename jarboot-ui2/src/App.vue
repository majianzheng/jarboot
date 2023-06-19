<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router';
import { zhCn, zhTw, en } from 'element-plus/lib/locale/index';
import { useI18n } from 'vue-i18n';
import { computed, onMounted, onUnmounted } from 'vue';
import { useBasicStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';

const route = useRoute();
const { locale } = useI18n();
const locales = { 'zh-CN': zhCn, 'zh-TW': zhTw, 'en-US': en } as any;
const language = computed(() => locales[locale.value]);
const basic = useBasicStore();
onMounted(() => {
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
  CommonUtils.init();
});
onUnmounted(() => {
  window.onresize = null;
});
</script>

<template>
  <el-config-provider :locale="language">
    <router-view v-slot="{ Component }">
      <transition name="slide-fade">
        <keep-alive>
          <component :is="Component" :key="route.path" v-if="route.meta.keepAlive" />
        </keep-alive>
      </transition>
      <component :is="Component" :key="route.path" v-if="!route.meta.keepAlive" />
    </router-view>
  </el-config-provider>
</template>

<style lang="less" scoped></style>
