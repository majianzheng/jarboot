<template>
  <div v-loading="state.loading" class="term-main" ref="termRef" :style="{ width: width + 'px', height: height + 'px' }"></div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import 'xterm/css/xterm.css';
import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import { FitAddon } from 'xterm-addon-fit';
import { CanvasAddon } from 'xterm-addon-canvas';
import { WebglAddon } from 'xterm-addon-webgl';
import { WebLinksAddon } from 'xterm-addon-web-links';
import { Unicode11Addon } from 'xterm-addon-unicode11';
import { SerializeAddon } from 'xterm-addon-serialize';
import { SearchAddon } from 'xterm-addon-search';
import CommonUtils from '@/common/CommonUtils';
import { debounce, floor } from 'lodash';
import { useUserStore } from '@/stores';

//单个字符宽高： width: 8 height: 16
const props = defineProps<{
  width: number;
  height: number;
  useWebgl?: boolean;
  host?: string;
}>();
interface TermOption {
  term: null | Terminal;
  fitAddon: null | FitAddon;
  websocket: null | WebSocket;
}
const emit = defineEmits<{
  (e: 'connected', option: TermOption): void;
  (e: 'disconnected', option: TermOption): void;
}>();
const userStore = useUserStore();

const CHAR_WIDTH = 8;
const CHAR_HEIGHT = 16;

const state = reactive({
  loading: true,
  connected: false,
});

const termRef = ref<HTMLDivElement>();

const termOption: TermOption = {
  term: null,
  websocket: null,
  fitAddon: null,
};

watch(() => [props.height, props.width], debounce(updateSize, 1000, { maxWait: 3000 }));

function updateSize() {
  termOption.fitAddon?.fit();
  if (state.connected) {
    const winSize = { col: getCol(), row: getRow() };
    const msg = JSON.stringify(winSize);
    termOption.websocket && termOption.websocket.send(new Blob([msg], { type: 'text/plain' }));
  }
}

function getCol() {
  return floor(props.width / CHAR_WIDTH);
}

function getRow() {
  return floor(props.height / CHAR_HEIGHT) - 1;
}

function createSocket() {
  if (termOption.websocket) {
    return termOption.websocket;
  }
  const token = `${CommonUtils.ACCESS_TOKEN}=${CommonUtils.getRawToken()}`;
  const query = `col=${getCol()}&row=${getRow()}&userDir=${userStore.userDir}`;
  const url = import.meta.env.DEV
    ? `ws://${window.location.hostname}:9899/jarboot/main/terminal/ws?${token}&${query}`
    : `ws://${window.location.host}/jarboot/main/terminal/ws?${token}&${query}`;
  console.info('terminal connect to ' + url);
  termOption.websocket = new WebSocket(url);
  termOption.websocket.onopen = () => {
    state.loading = false;
    state.connected = true;
    emit('connected', termOption);
    console.info('终端服务连接成功！');
  };
  termOption.websocket.onerror = error => {
    state.loading = false;
    state.connected = false;
    termOption.term?.writeln('连接出现错误！');
    emit('disconnected', termOption);
    console.error('连接异常', error);
  };
  termOption.websocket.onclose = () => {
    state.loading = false;
    state.connected = false;
    termOption.term?.writeln('连接断开！');
    emit('disconnected', termOption);
  };
  return termOption.websocket;
}

function init() {
  termOption.term = new Terminal({
    fontSize: 14,
    cursorBlink: true,
    allowProposedApi: true,
    convertEol: true,
    cols: getCol(),
    rows: getRow(),
    theme: {
      background: '#263238',
    },
  });
  const attachAddon = new AttachAddon(createSocket());

  // Attach the socket to term
  termOption.term.loadAddon(attachAddon);
  termOption.fitAddon = new FitAddon();
  termOption.term.loadAddon(termOption.fitAddon);
  termOption.term.loadAddon(new CanvasAddon());
  if (props.useWebgl) {
    const addon = new WebglAddon();
    termOption.term.loadAddon(addon);
  }
  termOption.term.loadAddon(new WebLinksAddon());
  const unicode11Addon = new Unicode11Addon();
  termOption.term.loadAddon(unicode11Addon);
  termOption.term.unicode.activeVersion = '11';
  const serializeAddon = new SerializeAddon();
  termOption.term.loadAddon(serializeAddon);
  const searchAddon = new SearchAddon();
  termOption.term.loadAddon(searchAddon);
  termOption.term.open(termRef.value as HTMLDivElement);
  termOption.fitAddon.fit();
  termOption.term.focus();
}

function fit() {
  termOption.fitAddon?.fit();
}

function focus() {
  termOption.term?.focus();
}

defineExpose({
  fit,
  focus,
});

onMounted(init);
onUnmounted(() => {
  console.info('onUnmounted terminal.');
  termOption.term?.dispose();
  termOption.websocket?.close();
});
</script>

<style scoped>
.term-main {
  width: 100%;
  min-height: 100px;
  background: #263238;
}
</style>
