<template>
  <code-mirror
    v-if="content.visible"
    :value="content.value"
    :options="options"
    @focus="emit('focus')"
    @contentChanged="onChange"
    :height="editorHeight"
    @ready="emit('ready')"></code-mirror>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, reactive, watch } from 'vue';

// require styles
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/material.css';
import 'codemirror/addon/display/fullscreen.css';
import 'codemirror/addon/display/fullscreen.js';
import 'codemirror/addon/fold/foldgutter.css';
import 'codemirror/addon/fold/foldcode.js';
import 'codemirror/addon/fold/foldgutter.js';
import 'codemirror/addon/fold/brace-fold.js';
import 'codemirror/addon/fold/comment-fold.js';
import 'codemirror/addon/selection/active-line';

import { parseModeByFilename } from '@/components/editor/LangUtils';

const props = defineProps<{
  /** 文件名 */
  name: string;
  /** 内容 */
  modelValue?: string;
  readonly?: boolean;
  fullScreen?: boolean;
  height?: string | number;
  /** 自定义显示模式，默认根据文件名推测 */
  mode?: string | { name: string };
}>();

const content = reactive({
  visible: false,
  value: '',
});
const editorHeight = computed(() => {
  if (!props.height) {
    return '400px';
  }
  return 'string' === typeof props.height ? props.height : `${props.height}px`;
});

const options = reactive({
  tabSize: 4,
  mode: { name: 'java' } as any,
  theme: 'material',
  line: true,
  connect: 'align',
  lineNumbers: true,
  collapseIdentical: false,
  highlightDifferences: true,
  readOnly: false,
  styleActiveLine: true,
  lineWrapping: true,
  foldGutter: true,
  gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
  fullScreen: false,
});

const emit = defineEmits<{
  (e: 'change', content: string): void;
  (e: 'focus'): void;
  (e: 'ready'): void;
  (e: 'update:modelValue', value: string): void;
}>();
const onChange = (value: string) => {
  emit('change', value);
  emit('update:modelValue', value);
};

function init() {
  options.readOnly = props.readonly || false;
  options.fullScreen = props.fullScreen || false;
  if (props.mode) {
    options.mode = props.mode;
  } else {
    options.mode = parseModeByFilename(props.name);
  }
  console.info('>>>>', options.mode);
  nextTick(() => (content.value = props.modelValue || ''));
  content.visible = true;
}

watch(() => [props.name, props.mode], init);

onMounted(init);
</script>

<style scoped></style>
