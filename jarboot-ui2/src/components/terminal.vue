<template>
  <div class="term-main" ref="termRef" :style="{ width: width + 'px', height: height + 'px' }"></div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
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

//单个字符宽高： width: 8 height: 16
const props = defineProps<{
  width: number;
  height: number;
  host?: string;
}>();
const CHAR_WIDTH = 8;
const CHAR_HEIGHT = 16;

const termRef = ref<HTMLDivElement>();

let term: Terminal;
let websocket: WebSocket;
let fitAddon: FitAddon;

watch(() => [props.height, props.width], debounce(updateSize, 1000, { maxWait: 3000 }));

function updateSize() {
  fitAddon.fit();
  const winSize = { col: getCol(), row: getRow() };
  const msg = JSON.stringify(winSize);
  websocket && websocket.send(new Blob([msg], { type: 'text/plain' }));
}

function getCol() {
  return floor(props.width / CHAR_WIDTH);
}

function getRow() {
  return floor(props.height / CHAR_HEIGHT) - 1;
}

function createSocket() {
  const token = `${CommonUtils.ACCESS_TOKEN}=${CommonUtils.getRawToken()}`;
  const query = `col=${getCol()}&row=${getRow()}`;
  const url = import.meta.env.DEV
    ? `ws://${window.location.hostname}:9899/jarboot/main/terminal/ws?${token}&${query}`
    : `ws://${window.location.host}/jarboot/main/terminal/ws?${token}&${query}`;
  console.info('terminal connect to ' + url);
  websocket = new WebSocket(url);
  websocket.onopen = () => {
    console.info('终端服务连接成功！');
  };
  websocket.onerror = error => {
    console.error('连接异常', error);
    term.writeln('连接出现错误！');
  };
  return websocket;
}

function init() {
  term = new Terminal({
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
  term.loadAddon(attachAddon);
  fitAddon = new FitAddon();
  term.loadAddon(fitAddon);
  term.loadAddon(new CanvasAddon());
  const addon = new WebglAddon();
  addon.onContextLoss(() => addon.dispose());
  term.loadAddon(addon);
  term.loadAddon(new WebLinksAddon());
  const unicode11Addon = new Unicode11Addon();
  term.loadAddon(unicode11Addon);
  term.unicode.activeVersion = '11';
  const serializeAddon = new SerializeAddon();
  term.loadAddon(serializeAddon);
  const searchAddon = new SearchAddon();
  term.loadAddon(searchAddon);
  term.open(termRef.value as HTMLDivElement);
  fitAddon.fit();
  term.focus();
  console.info('>>>modes:', term.modes);
}

onMounted(init);
</script>

<style scoped>
.term-main {
  width: 100%;
  min-height: 100px;
}
</style>
