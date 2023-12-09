<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch, nextTick } from 'vue';
import 'xterm/css/xterm.css';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { CanvasAddon } from 'xterm-addon-canvas';
import { WebLinksAddon } from 'xterm-addon-web-links';
import { Unicode11Addon } from 'xterm-addon-unicode11';
import { SerializeAddon } from 'xterm-addon-serialize';
import { SearchAddon } from 'xterm-addon-search';
import { debounce, floor } from 'lodash';
import { CONSOLE_TOPIC } from '@/types';
import type PublishSubmit from '@/common/PublishSubmit';

const props = defineProps<{
  width: number;
  height: number;
  useWebgl?: boolean;
  host?: string;
  pubsub?: PublishSubmit;
  /** 唯一id */
  id: string;
  executing?: string|null;
}>();

const emit = defineEmits<{
  (e: 'ready', terminal: Terminal): void;
  (e: 'command', value: string): void;
  (e: 'cancel'): void;
  (e: 'up'): void;
  (e: 'down'): void;
}>();

const CHAR_WIDTH = 8;
const CHAR_HEIGHT = 16;

const state = reactive({
  loading: true,
  connected: false,
});

const termOption = {
  term: null as unknown as Terminal | any,
  fitAddon: null as any,
};

const termRef = ref<HTMLDivElement>();
watch(() => [props.height, props.width], debounce(updateSize, 1000, { maxWait: 3000 }));

function getCol() {
  return floor(props.width / CHAR_WIDTH);
}

function getRow() {
  return floor(props.height / CHAR_HEIGHT) - 1;
}

function updateSize() {
  termOption.term.resize(getCol(), getRow());
  termOption.fitAddon?.fit();
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
  termOption.fitAddon = new FitAddon();
  termOption.term.loadAddon(termOption.fitAddon);
  termOption.term.loadAddon(new CanvasAddon());
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
  emit('ready', termOption.term);
  state.loading = false;
  const { pubsub, id } = props;
  if (pubsub) {
    pubsub.submit(id, CONSOLE_TOPIC.APPEND_LINE, onConsole);
    pubsub.submit(id, CONSOLE_TOPIC.STD_PRINT, onStdPrint);
    pubsub.submit(id, CONSOLE_TOPIC.BACKSPACE, onStdPrint);
    pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_END, scrollToEnd);
    pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_TOP, scrollToTop);
    pubsub.submit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, onClear);
  }
  termOption.term.prompt = prompt;
  termOption.term.setCurrent = setCurrent;
  runTerminal();
  prompt();
}

function fit() {
  termOption.fitAddon?.fit();
}

function focus() {
  termOption.term?.focus();
}

function runTerminal() {
  const term = termOption.term;
  term.writeln(banner());
  term.writeln('  Jarboot console, docs: [36mhttps://www.yuque.com/jarboot/usage/quick-start[0m');
  term.writeln('  Diagnose command, try running `help`.');

  term.onData((e: string) => {
    switch (e) {
      case '\u0003': // Ctrl+C
        emit('cancel');
        term.write('^C');
        prompt();
        break;
      case '\r': // Enter
        if (props.executing) {
          return;
        }
        runCommand(term, command);
        command = '';
        break;
      case '\u007F': // Backspace (DEL)
        if (props.executing) {
          return;
        }
        // Does not delete the prompt
        if (term._core.buffer.x > 2) {
          term.write('\b \b');
          if (command.length > 0) {
            command = command.substring(0, command.length - 1);
          }
        }
        break;
      case '[A': // 上
        if (props.executing) {
          return;
        }
        emit('up');
        break;
      case '[B': // 下
        if (props.executing) {
          return;
        }
        emit('down');
        break;
      default: // Print all other characters for demo
        if (props.executing) {
          return;
        }
        if ((e >= String.fromCharCode(0x20) && e <= String.fromCharCode(0x7e)) || e >= '\u00a0') {
          command += e;
          term.write(e);
        }
    }
  });
}

function setCurrent(str: string) {
  const term = termOption.term;
  let len = term._core.buffer.x;
  for (; len > 2; --len) {
    term.write('\b \b');
  }
  if (str) {
    term.write(str);
    command = str;
  } else {
    command = '';
  }
}

function prompt() {
  command = '';
  termOption.term.write('\r\n$ ');
}

let command = '';

function runCommand(term: Terminal, text: string) {
  const cmd = text.trim().split(' ')[0];
  if (cmd.length > 0) {
    term.writeln('');
    emit('command', text);
    return;
  }
  prompt();
}

function onConsole(line: string | undefined) {
  if (line) {
    nextTick(() => termOption.term.writeln(line));
  }
}
function onStdPrint(str: string) {
  nextTick(() => termOption.term.write(str));
}

function banner() {
  return (
    '[31m     ,--.[0m[32m        [0m[33m       [0m[34m,--.   [0m[35m       [0m[36m       [0m[31m  ,--.   [0m\n' +
    "[31m     |  |[0m[32m ,--,--.[0m[33m,--.--.[0m[34m|  |-. [0m[35m ,---. [0m[36m ,---. [0m[31m,-'  '-. [0m\n" +
    "[31m,--. |  |[0m[32m' ,-.  |[0m[33m|  .--'[0m[34m| .-. '[0m[35m| .-. |[0m[36m| .-. |[0m[31m'-.  .-' [0m\n" +
    "[31m|  '-'  /[0m[32m\\ '-'  |[0m[33m|  |  [0m[34m | `-' |[0m[35m' '-' '[0m[36m' '-' '[0m[31m  |  |   [0m\n" +
    "[31m `-----' [0m[32m `--`--'[0m[33m`--'   [0m[34m `---' [0m[35m `---' [0m[36m `---' [0m[31m  `--'   [0m\n"
  );
}

function scrollToEnd() {
  termOption.term.scrollToBottom();
}

function scrollToTop() {
  termOption.term.scrollToTop();
}

function onClear() {
  termOption.term.clear();
}

defineExpose({
  fit,
  focus,
  prompt,
});

onMounted(init);
onUnmounted(() => {
  console.info('onUnmounted terminal.');
  const { pubsub, id } = props;
  if (pubsub) {
    pubsub.unSubmit(id, CONSOLE_TOPIC.APPEND_LINE, onConsole);
    pubsub.unSubmit(id, CONSOLE_TOPIC.STD_PRINT, onStdPrint);
    pubsub.unSubmit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, onClear);
    pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_END, scrollToEnd);
    pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_TOP, scrollToTop);
  }
  try {
    termOption.term?.dispose();
  } catch (e) {
    console.warn(e);
  }
});
</script>

<template>
  <div v-loading="state.loading" class="term-main" ref="termRef" :style="{ width: width + 'px', height: height + 'px' }"></div>
</template>

<style scoped lang="less">
.term-main {
  width: 100%;
  min-height: 100px;
  background: #263238;
}
</style>
