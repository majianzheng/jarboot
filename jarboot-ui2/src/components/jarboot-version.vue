<template>
  <div class="version-title" v-show="versionStore.version">v{{ versionStore.version }}</div>
</template>

<script setup lang="ts">
import CloudService from '@/services/CloudService';
import { onMounted } from 'vue';
import { useBasicStore } from '@/stores';

const versionStore = useBasicStore();
onMounted(() => {
  CloudService.getVersion().then(resp => versionStore.setVersion(resp));
});
</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
