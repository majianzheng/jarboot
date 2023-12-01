<template>
  <div class="version-title" v-show="store.version">v{{ store.version + (store.inDocker ? '(Docker)' : '') }}</div>
</template>

<script setup lang="ts">
import { useBasicStore } from '@/stores';

const store = useBasicStore();
</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
