<template>
  <div class="theme-switch-wrap">
    <div class="theme-switch" @click="toggleDark()">
      <div class="theme-icon light" :style="{opacity: isDark ? 0 : 1}">
        <i class="iconfont icon-light"></i>
      </div>
      <div style="flex: auto;"></div>
      <div class="theme-icon" :style="{opacity: isDark ? 1 : 0}">
        <i class="iconfont icon-dark"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useDark, useToggle } from '@vueuse/core';
const isDark = useDark()
const toggleDark = useToggle(isDark)
</script>

<style lang="less" scoped>
.theme-switch-wrap {
  display: inline-block;
  height: 24px;
}
.theme-switch {
  display: flex;
  border-radius: 11px;
  width: 40px;
  height: 22px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color);
  box-sizing: border-box;
  transition: border-color .25s var(--el-transition-duration),background-color var(--el-transition-duration-fast);
  .theme-icon {
    padding: 1px;
    border: none;
    border-radius: 50%;
    transition: color var(--el-transition-duration),opacity var(--el-transition-duration);
    &.light {
      background: var(--el-bg-color);
      color: var(--el-text-color-regular);
    }
  }
}
</style>