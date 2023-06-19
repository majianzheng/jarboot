<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue';
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
import PublishSubmit from '@/common/PublishSubmit';

const props = defineProps<{
  width: number;
  height: number;
  useWebgl?: boolean;
  host?: string;
  pubsub?: PublishSubmit;
  /** 唯一id */
  id: string;
  prefix: string;
}>();

const emit = defineEmits<{
  (e: 'ready', terminal: Terminal): void;
  (e: 'command', value: string): void;
}>();

const CHAR_WIDTH = 8;
const CHAR_HEIGHT = 16;

const state = reactive({
  loading: true,
  connected: false,
});

const termOption = {
  term: null as unknown as Terminal,
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
  }
  runTerminal();
}

function fit() {
  termOption.fitAddon?.fit();
}

function focus() {
  termOption.term?.focus();
}

function runTerminal() {
  const term = termOption.term;

  term.prompt = () => {
    term.write('\r\n$ ');
  };

  // TODO: Use a nicer default font
  term.writeln(
    [
      '    Xterm.js is the frontend component that powers many terminals including',
      '                           \x1b[3mVS Code\x1b[0m, \x1b[3mHyper\x1b[0m and \x1b[3mTheia\x1b[0m!',
      '',
      ' ┌ \x1b[1mFeatures\x1b[0m ──────────────────────────────────────────────────────────────────┐',
      ' │                                                                            │',
      ' │  \x1b[31;1mApps just work                         \x1b[32mPerformance\x1b[0m                        │',
      ' │   Xterm.js works with most terminal      Xterm.js is fast and includes an  │',
      ' │   apps like bash, vim and tmux           optional \x1b[3mWebGL renderer\x1b[0m           │',
      ' │                                                                            │',
      ' │  \x1b[33;1mAccessible                             \x1b[34mSelf-contained\x1b[0m                     │',
      ' │   A screen reader mode is available      Zero external dependencies        │',
      ' │                                                                            │',
      ' │  \x1b[35;1mUnicode support                        \x1b[36mAnd much more...\x1b[0m                   │',
      ' │   Supports CJK 語 and emoji \u2764\ufe0f            \x1b[3mLinks\x1b[0m, \x1b[3mthemes\x1b[0m, \x1b[3maddons\x1b[0m,            │',
      ' │                                          \x1b[3mtyped API\x1b[0m, \x1b[3mdecorations\x1b[0m            │',
      ' │                                                                            │',
      ' └────────────────────────────────────────────────────────────────────────────┘',
      '',
    ].join('\n\r')
  );

  term.writeln('Below is a simple emulated backend, try running `help`.');
  prompt();

  term.onData(e => {
    switch (e) {
      case '\u0003': // Ctrl+C
        term.write('^C');
        prompt();
        break;
      case '\r': // Enter
        runCommand(term, command);
        command = '';
        break;
      case '\u007F': // Backspace (DEL)
        // Do not delete the prompt
        if (term._core.buffer.x > 2) {
          term.write('\b \b');
          if (command.length > 0) {
            command = command.substr(0, command.length - 1);
          }
        }
        break;
      default: // Print all other characters for demo
        if ((e >= String.fromCharCode(0x20) && e <= String.fromCharCode(0x7e)) || e >= '\u00a0') {
          command += e;
          term.write(e);
        }
    }
  });
}

function prompt() {
  command = '';
  termOption.term.write('\r\n$ ');
}

let command = '';

function runCommand(term: Terminal, text: string) {
  const command = text.trim().split(' ')[0];
  if (command.length > 0) {
    term.writeln('');
    emit('command', text);
  }
  prompt();
}

function onConsole(line: string | undefined) {
  if (line) {
    termOption.term.writeln(line);
    prompt();
    fit();
  }
}
function onStdPrint(str: string) {
  termOption.term.write(str);
  fit();
}

defineExpose({
  fit,
  focus,
});

onMounted(init);
onUnmounted(() => {
  console.info('onUnmounted terminal.');
  const { pubsub, id } = props;
  if (pubsub) {
    pubsub.unSubmit(id, CONSOLE_TOPIC.APPEND_LINE, onConsole);
    pubsub.unSubmit(id, CONSOLE_TOPIC.STD_PRINT, onStdPrint);
    pubsub.unSubmit(id, CONSOLE_TOPIC.BACKSPACE, onStdPrint);
    // pubsub.unSubmit(id, CONSOLE_TOPIC.START_LOADING, onStartLoading);
    // pubsub.unSubmit(id, CONSOLE_TOPIC.FINISH_LOADING, onFinishLoading);
    // pubsub.unSubmit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, onClear);
    // pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_END, scrollToEnd);
    // pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_TOP, scrollToTop);
  }
  termOption.term?.dispose();
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
