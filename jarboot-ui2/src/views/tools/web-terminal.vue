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
  width: basicStore.innerWidth - 80,
  height: basicStore.innerHeight - 70,
});
watch(() => [basicStore.innerHeight, basicStore.innerWidth], debounce(resize, 1500, { maxWait: 3000 }));

function resize() {
  // ignore
  state.width = basicStore.innerWidth - 80;
  state.height = basicStore.innerHeight - 70;
}
</script>

<style scoped></style>
