<script setup lang="ts">
import { useBasicStore } from '@/stores';

const basicStore = useBasicStore();
</script>

<template>
  <div>
    <terminal :width="basicStore.innerWidth" :height="basicStore.innerHeight" :use-webgl="true"></terminal>
  </div>
</template>

<style lang="less">
body {
  padding: 0 !important;
  margin: 0 !important;
  background: #263238;
}
#app {
  padding: 0 !important;
  margin: 0 !important;
  background: #263238;
}
</style>
