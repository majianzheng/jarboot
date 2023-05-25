<template>
  <svg v-bind="$attrs" class="___svg-icon" aria-hidden="true">
    <use :xlink:href="symbolId" />
  </svg>
</template>
<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  prefix: { type: String, default: 'icon' },
  icon: { type: String, required: true },
  color: { type: String, default: '' },
});

const symbolId = computed(() => `#${props.prefix}-${props.icon}`);
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
