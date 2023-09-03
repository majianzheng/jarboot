<template>
  <div class="version-title" v-show="versionStore.version">v{{ versionStore.version }}</div>
</template>

<script setup lang="ts">
import { useBasicStore } from '@/stores';

const versionStore = useBasicStore();
</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
