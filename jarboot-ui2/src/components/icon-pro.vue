<script setup lang="ts">
const props = defineProps<{
  icon: string;
  appendClass?: string;
}>();
</script>

<template>
  <el-icon v-if="icon.startsWith('icon-')" :class="appendClass || ''"><em class="iconfont" :class="icon"></em></el-icon>
  <el-icon v-else :class="appendClass || ''"><Component :is="icon" /></el-icon>
</template>
