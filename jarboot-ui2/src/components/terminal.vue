<template>
  <div class="term-main" ref="termRef"></div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import 'xterm/css/xterm.css';
import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import { FitAddon } from 'xterm-addon-fit';
import CommonUtils from '@/common/CommonUtils';
import { debounce } from 'lodash';
import { useBasicStore } from '@/stores';

const termRef = ref<HTMLDivElement>();
const basicStore = useBasicStore();

let term: Terminal;
let websocket: WebSocket;
let fitAddon: FitAddon;

watch(
  () => [basicStore.innerHeight, basicStore.innerWidth],
  debounce(() => fitAddon.fit(), 1000, { maxWait: 3000 })
);

function createSocket() {
  const token = `${CommonUtils.ACCESS_TOKEN}=${CommonUtils.getRawToken()}`;
  const url = import.meta.env.DEV
    ? `ws://${window.location.hostname}:9899/jarboot/main/terminal/ws?${token}`
    : `ws://${window.location.host}/jarboot/main/terminal/ws?${token}`;
  console.info('terminal connect to ' + url);
  websocket = new WebSocket(url);
  websocket.onerror = error => {
    console.error('连接异常', error);
  };
  return websocket;
}

function init() {
  term = new Terminal({
    fontSize: 14,
    cursorBlink: true,
    disableStdin: false,
    allowTransparency: true,
    allowProposedApi: true,
    convertEol: true,
    windowsMode: true,
    theme: {
      background: '#263238',
    },
  });
  const attachAddon = new AttachAddon(createSocket());

  // Attach the socket to term
  term.loadAddon(attachAddon);
  fitAddon = new FitAddon();
  term.loadAddon(fitAddon);
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
  min-height: 500px;
}
</style>
