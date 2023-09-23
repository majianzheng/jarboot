<template>
  <svg v-bind="$attrs" class="___svg-icon" aria-hidden="true" :style="style">
    <use :xlink:href="'#' + icon" />
  </svg>
</template>
<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  icon: string;
  color?: string;
  size?: number | string;
}>();

const style = computed(() => {
  let str = '';
  if (props.color) {
    str = `color:${props.color};`;
  }
  if (props.size) {
    str += `size:${props.size};`;
  }
  return str;
});
</script>

<style lang="less" scoped>
.___svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}
</style>
