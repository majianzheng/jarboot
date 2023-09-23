<script setup lang="ts">
defineProps<{
  icon: string;
  color?: string;
  size?: number | string;
  title?: string;
  svg?: boolean;
}>();
</script>

<template>
  <el-icon :color="color" :size="size || ''" :title="title">
    <svg-icon v-if="icon.startsWith('icon-') && svg" :icon="icon"></svg-icon>
    <em v-else-if="icon.startsWith('icon-')" class="iconfont" :class="icon"></em>
    <Component v-else :is="icon" />
  </el-icon>
</template>
