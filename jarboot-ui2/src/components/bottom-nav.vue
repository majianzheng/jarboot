<template>
  <el-space class="bottom-nav" :size="0" fill :fill-ratio="100 / basic.menus.length">
    <div v-for="(item, i) in basic.menus" :key="i">
      <div class="menu-btn" :class="{ 'nav-btn-active': isActive(item) }" @click="goTo(item)">
        <div><icon-pro :icon="item.icon" :size="22"></icon-pro></div>
        <div class="menu-title">{{ $t(item.module as string) }}</div>
      </div>
    </div>
  </el-space>
</template>

<script setup lang="ts">
import { useBasicStore } from '@/stores';
import { useRoute, useRouter } from 'vue-router';
import type { MenuItem } from '@/types';

const route = useRoute();
const router = useRouter();
const basic = useBasicStore();

const isActive = (menu: MenuItem): boolean => {
  if (route.name === menu.name) {
    return true;
  }
  return route.meta.module === menu.module;
};

const goTo = (menu: any) => {
  if (route.name !== menu.name && !isActive(menu)) {
    router.push({ name: menu.subName || menu.name });
  }
};
</script>

<style scoped>
.bottom-nav {
  width: 100vw;
  height: 50px;
  position: fixed;
  overflow: hidden;
  background: var(--el-bg-color);
  color: var(--bottom-nav-color);
  border-top: var(--el-border);
  top: calc(100vh - 50px);
  left: 0;
  right: 0;
  z-index: 1000;
  .menu-title {
    font-size: 12px;
  }
  .menu-btn {
    text-align: center;
    cursor: pointer;
    &.nav-btn-active {
      color: var(--el-color-primary);
      cursor: default;
    }
  }
}
</style>
