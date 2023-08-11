<template>
  <div class="version-title" v-show="versionStore.version">v{{ versionStore.version }}</div>
</template>

<script setup lang="ts">
import Request from '@/common/Request';
import { onMounted } from 'vue';
import { useBasicStore } from '@/stores';
import type { ServerRuntimeInfo } from '@/types';

const versionStore = useBasicStore();

onMounted(async () => {
  const runtimeInfo = await Request.get<ServerRuntimeInfo>(`/api/jarboot/public/serverRuntime`, {});
  versionStore.setVersion(runtimeInfo.version);
});
</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
