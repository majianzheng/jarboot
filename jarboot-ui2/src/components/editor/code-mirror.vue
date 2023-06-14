<template>
  <div class="vue-codemirror" :class="{ merge }">
    <div ref="mergeview" v-if="merge"></div>
    <textarea ref="textarea" :name="name" :placeholder="placeholder" v-else></textarea>
  </div>
</template>

<script lang="ts" setup>
import { watch, onMounted, onUnmounted, reactive, nextTick, ref } from 'vue';
// @ts-ignore
import _CodeMirror from 'codemirror';
// @ts-ignore
const CodeMirror = window.CodeMirror || _CodeMirror;

let codemirror = null as any;
let cminstance = null as any;
const mergeview = ref(null);
const textarea = ref(null);

const data = reactive({ content: '' });
const props = defineProps({
  code: String,
  value: String,
  marker: Function,
  unseenLines: Array,
  height: {
    type: String,
    default: '400px',
  },
  name: {
    type: String,
    default: 'codemirror',
  },
  placeholder: {
    type: String,
    default: '',
  },
  merge: {
    type: Boolean,
    default: false,
  },
  options: {
    type: Object as any,
    default: {} as any,
  },
  events: {
    type: Array,
    default: [] as any[],
  },
  globalOptions: {
    type: Object,
    default: {} as any,
  },
  globalEvents: {
    type: Array,
    default: [] as any[],
  },
});
const emit = defineEmits([
  'ready',
  'contentChanged',
  'input',
  'scroll',
  'changes',
  'beforeChange',
  'cursorActivity',
  'keyHandled',
  'inputRead',
  'electricInput',
  'beforeSelectionChange',
  'viewportChange',
  'swapDoc',
  'gutterClick',
  'gutterContextMenu',
  'focus',
  'blur',
  'refresh',
  'optionChange',
  'scrollCursorIntoView',
  'update',
  'save',
]);
const initialize = () => {
  const cmOptions = Object.assign({}, props.globalOptions, props.options);
  if (props.merge) {
    codemirror = CodeMirror.MergeView(mergeview.value, cmOptions);
    cminstance = codemirror.edit;
  } else {
    codemirror = CodeMirror.fromTextArea(textarea.value, cmOptions);
    cminstance = codemirror;
    const value = props.code || props.value || data?.content;
    if ('string' === typeof value) {
      cminstance.setValue(value);
    }
  }
  // @ts-ignore
  cminstance.on('change', cm => {
    data.content = cm.getValue();
    emit('input', data.content);
    emit('contentChanged', data.content);
  });
  // 所有有效事件（驼峰命名）+ 去重
  const tmpEvents = {} as any;
  [
    'scroll',
    'changes',
    'beforeChange',
    'cursorActivity',
    'keyHandled',
    'inputRead',
    'electricInput',
    'beforeSelectionChange',
    'viewportChange',
    'swapDoc',
    'gutterClick',
    'gutterContextMenu',
    'focus',
    'blur',
    'refresh',
    'optionChange',
    'scrollCursorIntoView',
    'update',
    'save',
  ]
    // @ts-ignore
    .concat(props.events || [])
    // @ts-ignore
    .concat(props.globalEvents || [])
    .filter(e => !tmpEvents[e] && (tmpEvents[e] = true))
    .forEach(event => {
      // 循环事件，并兼容 run-time 事件命名
      // @ts-ignore
      cminstance.on(event, (...args) => {
        // @ts-ignore
        emit(event, ...args);
      });
    });
  cminstance.display.wrapper.style.height = props.height;
  emit('ready', codemirror);
  unseenLineMarkers();
  // prevents funky dynamic rendering
  refresh();
};
const refresh = () => {
  if (!cminstance) {
    return;
  }
  nextTick(() => {
    cminstance.refresh();
  });
};
const destroy = () => {
  if (!cminstance) {
    return;
  }
  // garbage cleanup
  const element = cminstance.doc.cm.getWrapperElement();
  element && element.remove && element.remove();
};
const handeCodeChange = (newVal: string) => {
  if (!cminstance) {
    return;
  }
  const cm_value = cminstance.getValue();
  if (newVal !== cm_value) {
    const scrollInfo = cminstance.getScrollInfo();
    cminstance.setValue(newVal);
    data.content = newVal;
    cminstance.scrollTo(scrollInfo.left, scrollInfo.top);
  }
  unseenLineMarkers();
};

const unseenLineMarkers = () => {
  if (!cminstance || !props.unseenLines?.length || !props.marker) {
    return;
  }
  props.unseenLines.forEach(line => {
    const info = cminstance.lineInfo(line);
    // @ts-ignore
    cminstance.setGutterMarker(line, 'breakpoints', info.gutterMarkers ? null : props.marker());
  });
};
const switchMerge = () => {
  if (!cminstance) {
    return;
  }
  // Save current values
  const history = cminstance.doc.history;
  const cleanGeneration = cminstance.doc.cleanGeneration;
  props.options['value'] = cminstance.getValue() as any;
  destroy();
  initialize();
  // Restore values
  cminstance.doc.history = history;
  cminstance.doc.cleanGeneration = cleanGeneration;
};

watch(
  () => props.options,
  newOptions => {
    if (!cminstance) {
      return;
    }
    for (const key in newOptions) {
      cminstance.setOption(key, newOptions[key]);
    }
  }
);
watch(
  () => props.merge,
  () => {
    if (!cminstance) {
      return;
    }
    nextTick(switchMerge);
  }
);

watch(
  () => props.code,
  (newVal: any) => {
    if (!cminstance) {
      return;
    }
    handeCodeChange(newVal);
  }
);

watch(
  () => props.value,
  (newVal: any) => {
    if (!cminstance) {
      return;
    }
    handeCodeChange(newVal);
  }
);

onMounted(initialize);
onUnmounted(destroy);
</script>
