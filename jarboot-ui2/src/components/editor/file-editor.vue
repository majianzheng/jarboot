<template>
  <code-mirror
    v-if="content.visible"
    :value="content.value"
    :options="options"
    @focus="emit('focus')"
    @contentChanged="onChange"
    @keydown.stop="onKey"
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
  visible: true,
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
  (e: 'save'): void;
  (e: 'search'): void;
  (e: 'change', content: string): void;
  (e: 'focus'): void;
  (e: 'ready'): void;
  (e: 'update:modelValue', value: string): void;
}>();
const onChange = (value: string) => {
  emit('change', value);
  emit('update:modelValue', value);
};

function onKey(event: KeyboardEvent) {
  let ctl = false;
  if (window.navigator.userAgent.includes('Mac OS')) {
    ctl = event.metaKey;
  } else {
    ctl = event.ctrlKey;
  }
  if (ctl) {
    if ('KeyF' === event.code) {
      // 搜索
      emit('search');
      event.preventDefault();
      return;
    }
    if ('KeyS' === event.code) {
      // 保存
      emit('save');
      event.preventDefault();
      return;
    }
  }
}

function init() {
  content.visible = false;
  options.readOnly = props.readonly || false;
  options.fullScreen = props.fullScreen || false;
  if (props.mode) {
    options.mode = props.mode;
  } else {
    options.mode = parseModeByFilename(props.name);
  }
  content.value = props.modelValue || '';
  nextTick(() => (content.visible = true));
}

watch(() => [props.name, props.mode], init);

onMounted(init);
</script>
