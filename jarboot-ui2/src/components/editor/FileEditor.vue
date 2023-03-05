<template>
  <codemirror
      v-model="props.modelValue"
      placeholder="Code goes here..."
      :style="{ height: props.height || '400px', width: '100%' }"
      :autofocus="true"
      :indent-with-tab="true"
      :tab-size="state.options.tabSize"
      :extensions="extensions"
      @ready="$emit('ready')"
      @change="onChange"
      @focus="$emit('focus')"
      @blur="$emit('blur', $event)"
  />
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';
import { Codemirror } from 'vue-codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'

const props = defineProps<{
  /** 文件名 */
  name: string;
  /** 内容 */
  modelValue: string;
  height?: string
  /** 自定义显示模式，默认根据文件名推测 */
  mode?: string|undefined;
}>();

const state = ref( {
  content: '',
  options: {
    tabSize: 4,
    mode: 'text/javascript',
    theme: 'base16-dark',
    lineNumbers: true,
    line: true,
  }
});
const emit = defineEmits<{
  (e: 'change', content: string): void
  (e: 'update:modelValue', value: string): void
}>();
const onChange = (content: string) => {
  emit('change', content);
  emit('update:modelValue', content);
};

const extensions = [javascript(), oneDark];

onMounted(() => {
  if (props.mode) {
    state.value.options.mode = props.mode;
  } else {
    // 根据文件名推测模式
  }
});

</script>

<style scoped>

</style>