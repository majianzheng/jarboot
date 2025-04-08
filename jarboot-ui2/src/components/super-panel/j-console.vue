<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch, nextTick } from 'vue';
import '@xterm/xterm/css/xterm.css';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { CanvasAddon } from '@xterm/addon-canvas';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { Unicode11Addon } from '@xterm/addon-unicode11';
import { SerializeAddon } from '@xterm/addon-serialize';
import { type ISearchOptions, SearchAddon } from '@xterm/addon-search';
import { debounce, floor } from 'lodash';
import wcwidth from 'wcwidth';
import { CONSOLE_TOPIC } from '@/types';
import type PublishSubmit from '@/common/PublishSubmit';
import { ElMessage } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';

const props = defineProps<{
  width: number;
  height: number;
  useWebgl?: boolean;
  host?: string;
  pubsub?: PublishSubmit;
  /** 唯一id */
  id: string;
  executing?: string | null;
}>();
interface TermOption {
  term: null | Terminal;
  fitAddon: null | FitAddon;
  searchAddon: null | SearchAddon;
  curseIndex: number;
}
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
  search: '',
  searchView: false,
  searchOpt: {
    caseSensitive: false,
    wholeWord: false,
    regex: false,
    decorations: {
      matchBackground: 'rgb(97,51,21)',
      matchBorder: '',
      matchOverviewRuler: '',
      activeMatchBackground: 'rgb(157,105,3)',
      activeMatchBorder: '',
      activeMatchOverviewRuler: '',
      activeMatchColorOverviewRuler: '',
    },
  } as ISearchOptions,
  searchResult: {
    cnt: 0,
    index: -1,
  },
});

const termOption: TermOption = {
  term: null as unknown as Terminal | any,
  searchAddon: null,
  fitAddon: null as any,
  curseIndex: 0,
};

const termRef = ref<HTMLDivElement>();
watch(() => [props.height, props.width], debounce(updateSize, 1000, { maxWait: 3000 }));

const searchInputRef = ref<HTMLInputElement>();
function clearSearchResult() {
  if (state.searchResult.cnt > 0) {
    termOption.searchAddon?.clearDecorations();
    state.searchResult.cnt = 0;
    state.searchResult.index = -1;
  }
}

const findPrevious = debounce(
  function findPrevious() {
    if (state.search) {
      termOption.searchAddon?.findPrevious(state.search, state.searchOpt);
    } else {
      clearSearchResult();
    }
  },
  300,
  { maxWait: 500 }
);

const findNext = debounce(
  function findNext() {
    if (state.search) {
      termOption.searchAddon?.findNext(state.search, state.searchOpt);
    } else {
      clearSearchResult();
    }
  },
  300,
  { maxWait: 500 }
);

function toggleCaseSensitive() {
  state.searchOpt.caseSensitive = !state.searchOpt.caseSensitive;
  findNext();
}
function toggleRegex() {
  state.searchOpt.regex = !state.searchOpt.regex;
  findNext();
}
function toggleWholeWord() {
  state.searchOpt.wholeWord = !state.searchOpt.wholeWord;
  findNext();
}
function toggleOpt(evt: KeyboardEvent) {
  if (!evt.altKey) {
    return;
  }
  switch (evt.code) {
    case 'KeyC':
      toggleCaseSensitive();
      break;
    case 'KeyW':
      toggleWholeWord();
      break;
    case 'KeyR':
      toggleRegex();
      break;
    default:
      break;
  }
}
function openSearch() {
  state.searchView = true;
  findNext();
  nextTick(() => searchInputRef.value?.focus());
}
function closeSearch() {
  state.searchView = false;
  clearSearchResult();
}

function getCol() {
  return floor(props.width / CHAR_WIDTH);
}

function getRow() {
  return floor(props.height / CHAR_HEIGHT) - 1;
}

function updateSize() {
  termOption.term?.resize(getCol(), getRow());
  termOption.fitAddon?.fit();
}

function init() {
  termOption.term = new Terminal({
    fontSize: 14,
    cursorBlink: true,
    allowProposedApi: true,
    windowsMode: false,
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
  termOption.searchAddon = searchAddon;
  termOption.term.loadAddon(searchAddon);
  termOption.term.open(termRef.value as HTMLDivElement);
  termOption.fitAddon.fit();
  termOption.term.focus();
  searchAddon.onDidChangeResults(evt => {
    state.searchResult.index = evt.resultIndex + 1;
    state.searchResult.cnt = evt.resultCount;
  });

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

function copySelection() {
  const str = termOption.term?.getSelection();
  if (!str) {
    console.info('No selection');
    return;
  }
  CommonUtils.copyString(str);
  ElMessage({
    message: CommonUtils.translate('COPIED'),
    type: 'success',
  });
}

function backspace() {
  const term = termOption.term;
  if (!term) {
    return;
  }
  // Does not delete the prompt
  let index = termOption.curseIndex;
  if (index > 0) {
    const rawCmd = command;
    let text = '';
    let isWide = false;
    if (index > 1) {
      command = rawCmd.substring(0, index - 1);
      isWide = isWideChar(rawCmd.charAt(index - 1));
      if (index <= rawCmd.length - 1) {
        text = rawCmd.substring(index);
        command += text;
      }
    } else {
      text = rawCmd.substring(index);
      isWide = isWideChar(rawCmd.charAt(0));
      command = text;
    }

    // 重绘输入内容
    term.write('\x1b[D'); // 左移一位
    if (isWide) {
      term.write('\x1b[D');
    }
    term.write(`\x1b[K`); // 清除右侧内容
    termOption.curseIndex--;
    if (text.length > 0) {
      term.write(text); // 写入剩余字符
      // 调整光标位置
      term.write(`\x1b[${calcTextWidth(text)}D`);
    }
  }
}
function deleteChar() {
  const term = termOption.term;
  if (!term) {
    return;
  }
  let index = termOption.curseIndex;
  if (index <= command.length - 1) {
    const rawCmd = command;
    let text = '';
    if (index >= 0) {
      if (index > 0) {
        command = rawCmd.substring(0, index);
      }
      if (index < rawCmd.length - 1) {
        text = rawCmd.substring(index + 1);
        command += text;
      }
    } else {
      text = rawCmd.substring(index + 1);
      command = text;
    }
    term.write(`\x1b[K`); // 清除右侧内容
    if (text.length > 0) {
      term.write(text); // 写入剩余字符
      // 调整光标位置
      term.write(`\x1b[${calcTextWidth(text)}D`);
    }
    console.info('>>>', text.length, index, command);
  }
}

function handleInput(term: Terminal, e: string) {
  if (props.executing) {
    return;
  }
  let index = term._core.buffer.x - 2;
  if (index < 0) {
    prompt();
  }
  index = termOption.curseIndex;
  if ((e >= String.fromCharCode(0x20) && e <= String.fromCharCode(0x7e)) || e >= '\u00a0') {
    // command的index索引处插入字符
    let append = e;
    if (index < command.length) {
      append += command.substring(index);
    }

    // 重新渲染输入内容
    term.write('\x1b[K'); // 清除当前行从光标到行尾的内容
    term.write(append);

    // 移动光标到正确位置
    const moveLeft = command.length - index;
    if (moveLeft > 0) {
      term.write(`\x1b[${moveLeft}D`); // 使用 ANSI 序列调整光标
    }
    if (index > 0) {
      command = command.substring(0, index) + append;
    } else {
      command = append;
    }
    termOption.curseIndex += e.length;
    console.info('command:', command, termOption.curseIndex);
  }
}

function getPrevCursorPos(line: string, currentPos: number) {
  if (currentPos <= 0) return 0;

  let pos = currentPos - 1;
  // 处理宽字符的左侧边界
  while (pos > 0 && isWideChar(line[pos - 1])) {
    pos--;
  }
  return pos;
}

function getNextCursorPos(line: string, currentPos: number) {
  if (currentPos >= line.length) return currentPos;

  let steps = 1;
  // 如果当前字符是宽字符，移动两步
  if (isWideChar(line[currentPos])) {
    steps = 2;
  }
  return Math.min(currentPos + steps, line.length);
}

function isWideChar(char: string) {
  return wcwidth(char) === 2;
}

function calcCmdWidth(): number {
  return calcTextWidth(command);
}

function calcTextWidth(str: string): number {
  if (str?.length) {
    let w = 0;
    for (const element of str) {
      if (isWideChar(element)) {
        w += 2;
      } else {
        w++;
      }
    }
    return w;
  } else {
    return 0;
  }
}

function handleCursorMove(direction: string) {
  const term = termOption.term;
  if (!term) {
    return;
  }
  // 获取当前光标位置和行内容
  const cursorX = term.buffer.active.cursorX;
  const cursorY = term.buffer.active.cursorY;
  const line = term.buffer.active.getLine(cursorY)?.translateToString() || '';

  // 计算新位置
  let newPos = direction === 'ArrowLeft' ? getPrevCursorPos(line, cursorX) : getNextCursorPos(line, cursorX);

  term.write(`\x1B[${cursorY + 1};${newPos + 1}H`);
}

function runTerminal() {
  const term = termOption.term;
  if (!term) {
    return;
  }
  term.writeln(banner());
  term.writeln('  Jarboot console, docs: [36mhttps://www.yuque.com/jarboot/usage/quick-start[0m');
  term.writeln('  Diagnose command, try running `help`.');
  let isComposing = false;

  term.textarea?.addEventListener('compositionstart', () => {
    isComposing = true;
  });

  term.textarea?.addEventListener('compositionend', () => {
    isComposing = false;
  });
  term.attachCustomKeyEventHandler((event: KeyboardEvent) => {
    if (isComposing) return false;
    if (event.type === 'keydown') {
      if ('Escape' === event.code && state.searchView) {
        closeSearch();
        return false;
      }
      let ctl = false;
      if (window.navigator.userAgent.includes('Mac OS')) {
        ctl = event.metaKey;
      } else {
        ctl = event.ctrlKey;
      }

      if (ctl) {
        if ('KeyF' === event.code) {
          openSearch();
          return false;
        }
        if ('KeyC' === event.code) {
          copySelection();
          emit('cancel');
          return false;
        }
        if ('KeyV' === event.code) {
          return false;
        }
        return '' !== event.code;
      }
      if (event.altKey && state.searchView) {
        if ('KeyC' === event.code) {
          state.searchOpt.caseSensitive = !state.searchOpt.caseSensitive;
          findNext();
          return false;
        }
        if ('KeyR' === event.code) {
          state.searchOpt.regex = !state.searchOpt.regex;
          findNext();
          return false;
        }
        if ('KeyW' === event.code) {
          state.searchOpt.wholeWord = !state.searchOpt.wholeWord;
          findNext();
          return false;
        }
      }
      if ('Backspace' === event.code) {
        backspace();
        return false;
      }
      if ('Delete' === event.code) {
        deleteChar();
        return false;
      }
      if ('ArrowUp' === event.code) {
        if (!props.executing) {
          emit('up');
        }
        return false;
      }
      if ('ArrowDown' === event.code) {
        if (!props.executing) {
          emit('down');
        }
        return false;
      }
      if ('ArrowLeft' === event.code) {
        if (term._core.buffer.x > 2) {
          term.write('\x1b[D');
          termOption.curseIndex--;
          console.info('isWideChar ', isWideChar(command[termOption.curseIndex]), command[termOption.curseIndex]);
          if (isWideChar(command[termOption.curseIndex])) {
            term.write('\x1b[D');
          }
        }
        console.info('curse index', term._core.buffer.x - 3, termOption.curseIndex);
        return false;
      }
      if ('ArrowRight' === event.code) {
        if (term._core.buffer.x < 2 + calcCmdWidth()) {
          term.write('\x1b[C');
          if (isWideChar(command[termOption.curseIndex])) {
            term.write('\x1b[C');
          }
          termOption.curseIndex++;
        }
        console.info('curse index', term._core.buffer.x - 3, termOption.curseIndex);
        return false;
      }
    }
    return true;
  });
  term.onData((e: string) => {
    switch (e) {
      case '\u0003': // Ctrl+C
        nextTick(copySelection);
        console.info('Ctrl+C');
        break;
      case '\r': // Enter
        if (props.executing) {
          return;
        }
        runCommand(term, command);
        command = '';
        termOption.curseIndex = 0;
        break;
      case '\u007F': // Backspace (DEL)
      case '[A': // 上
      case '[B': // 下
      case '[D': // 左
      case '[C': // 右
        break;
      default: // Print all other characters for demo
        handleInput(term, e);
    }
  });
}

function setCurrent(str: string) {
  const term = termOption.term;
  let len = term?._core.buffer.x;
  for (; len > 2; --len) {
    term?.write('\b \b');
  }
  if (str) {
    term?.write(str);
    command = str;
    termOption.curseIndex = str.length;
  } else {
    command = '';
    termOption.curseIndex = 0;
  }
}

function prompt() {
  command = '';
  termOption.term?.write('\r\n$ ');
  termOption.curseIndex = 0;
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
    nextTick(() => termOption.term?.writeln(line));
    command = '';
    termOption.curseIndex = 0;
  }
}
function onStdPrint(str: string) {
  nextTick(() => termOption.term?.write(str));
  command = '';
  termOption.curseIndex = 0;
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
  termOption.term?.scrollToBottom();
}

function scrollToTop() {
  termOption.term?.scrollToTop();
}

function onClear() {
  termOption.term?.clear();
  prompt();
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
    termOption?.term?.dispose();
  } catch (e) {
    console.debug(e);
  }
});
</script>

<template>
  <div v-loading="state.loading" class="term-main" ref="termRef" :style="{ width: width + 'px', height: height + 'px' }"></div>
  <div class="search-view" :style="{ left: `${width - 640}px` }" v-if="state.searchView">
    <div style="display: flex">
      <div style="flex: auto">
        <el-input
          ref="searchInputRef"
          v-model="state.search"
          :placeholder="$t('SEARCH_BTN')"
          @input="findNext"
          prefix-icon="Search"
          size="small"
          @keydown.esc="closeSearch"
          @keydown.enter.exact="findNext"
          @keydown.shift.enter="findPrevious"
          @keydown="toggleOpt"
          clearable />
      </div>
      <div class="search-btn" @click="search">
        <div
          class="search-opt-tool"
          :title="$t('CASE_SENSITIVE')"
          :class="{ active: state.searchOpt.caseSensitive }"
          @click="toggleCaseSensitive">
          Cc
        </div>
        <div class="search-opt-tool" :title="$t('WHOLE_WORD')" :class="{ active: state.searchOpt.wholeWord }" @click="toggleWholeWord">W</div>
        <div class="search-opt-tool" :title="$t('REGEX')" :class="{ active: state.searchOpt.regex }" @click="toggleRegex">.*</div>

        <span class="search-result" v-if="state.searchResult.cnt > 0">{{ $t('SEARCH_RESULT', state.searchResult) }}</span>
        <span class="search-result" v-else>{{ $t('NO_RESULT') }}</span>
        <div class="search-opt-tool search-ext" :title="$t('PRE_SEARCH_RLT')" @click="findPrevious">
          <icon-pro icon="Top"></icon-pro>
        </div>
        <div class="search-opt-tool search-ext" :title="$t('NEXT_SEARCH_RLT')" @click="findNext">
          <icon-pro icon="Bottom"></icon-pro>
        </div>
        <div class="search-opt-tool search-ext" :title="$t('CLOSE')" @click="closeSearch">
          <icon-pro icon="CloseBold"></icon-pro>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="search-view-btn" :style="{ left: `${width - 40}px` }">
    <el-button :title="$t('SEARCH_BTN') + '(Ctrl+F)'" icon="Search" size="small" circle @click="openSearch"></el-button>
  </div>
</template>

<style scoped lang="less">
.term-main {
  width: 100%;
  min-height: 100px;
  background: #263238;
}

.search-view {
  position: absolute;
  top: 26px;
  width: 630px;
  z-index: 999;
  padding: 2px;
  border-radius: 3px;
  background: #263238;
  color: #e9e9eb;
}
.search-view-btn {
  position: absolute;
  top: 26px;
  background: #263238;
  z-index: 999;
}
.search-btn {
  margin-left: 2px;
  .search-opt-tool {
    display: inline-block;
    margin-right: 2px;
    font-size: 12px;
    width: 22px;
    cursor: pointer;
    border-radius: 3px;
    text-align: center;
    line-height: 22px;
    border: var(--el-border);
    &:hover {
      background-color: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary-light-7);
      color: var(--el-color-primary);
    }
    &.active {
      color: var(--el-text-color);
      border-color: var(--el-color-primary);
      background-color: var(--el-color-primary);
    }
  }
  .search-ext {
    font-size: 14px;
    margin-right: 10px;
    padding: 1px;
    border-style: dotted;
  }
}
.search-result {
  margin: 0 15px;
}
</style>
