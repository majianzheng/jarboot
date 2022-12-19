<template>
<div class="version-title" v-show="version">v{{version}}</div>
</template>

<script setup lang="ts">
import CloudService from "@/services/CloudService";
import {ref} from "vue";

const version = ref('');
CloudService.getVersion().then((resp) => version.value = resp as unknown as string);

</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>