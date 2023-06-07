<template>
  <div>
    <terminal :width="state.width" :height="state.height"></terminal>
  </div>
</template>

<script lang="ts" setup>
import { useBasicStore } from '@/stores';
import { reactive, watch } from 'vue';
import { debounce } from 'lodash';

const basicStore = useBasicStore();

const state = reactive({
  width: basicStore.innerWidth - 20,
  height: basicStore.innerHeight - 80,
});
watch(() => [basicStore.innerHeight, basicStore.innerWidth], debounce(resize, 1500, { maxWait: 3000 }));

function resize() {
  // ignore
  state.width = basicStore.innerWidth - 20;
  state.height = basicStore.innerHeight - 80;
}
</script>

<style scoped></style>
