<template>
  <div v-loading="state.loading" class="term-main" ref="termRef" :style="{ width: width + 'px', height: height + 'px' }"></div>
  <div class="search-view" v-if="state.searchView">
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
  <div v-else class="search-view-btn">
    <el-button :title="$t('SEARCH_BTN') + '(Ctrl+F)'" icon="Search" size="small" circle @click="openSearch"></el-button>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import '@xterm/xterm/css/xterm.css';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { CanvasAddon } from '@xterm/addon-canvas';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { Unicode11Addon } from '@xterm/addon-unicode11';
import { SerializeAddon } from '@xterm/addon-serialize';
import { SearchAddon, type ISearchOptions } from '@xterm/addon-search';
import { WebglAddon } from '@xterm/addon-webgl';
import { AttachAddon } from '@xterm/addon-attach';
import { debounce, floor } from 'lodash';
import { useUserStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';
import { ElMessage } from 'element-plus';

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
  searchAddon: null | SearchAddon;
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

const termRef = ref<HTMLDivElement>();

const termOption: TermOption = {
  term: null,
  websocket: null,
  searchAddon: null,
  fitAddon: null,
};

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

function updateSize() {
  termOption.term?.resize(getCol(), getRow());
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

function getDefaultHost() {
  return import.meta.env.DEV ? `${window.location.hostname}:9899` : `${window.location.host}`;
}

function createSocket() {
  if (termOption.websocket) {
    return termOption.websocket;
  }
  let query = `col=${getCol()}&row=${getRow()}&userDir=${userStore.userDir}`;
  if (props.host) {
    query += `&clusterHost=${props.host}`;
  }
  const protocol = 'https:' === window.location.protocol ? 'wss' : 'ws';
  const url = `${protocol}://${getDefaultHost()}/jarboot/main/terminal/ws?${query}`;
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
  termOption.searchAddon = searchAddon;
  termOption.term.loadAddon(searchAddon);
  termOption.term.open(termRef.value as HTMLDivElement);
  termOption.fitAddon.fit();
  termOption.term.focus();
  searchAddon.onDidChangeResults(evt => {
    state.searchResult.index = evt.resultIndex + 1;
    state.searchResult.cnt = evt.resultCount;
  });
  termOption.term.attachCustomKeyEventHandler(event => {
    if (event.type === 'keydown') {
      let ctl;
      if (window.navigator.userAgent.includes('Mac OS')) {
        ctl = event.metaKey;
      } else {
        ctl = event.ctrlKey;
      }
      if ('Escape' === event.code && state.searchView) {
        closeSearch();
        return false;
      }

      if (ctl) {
        if ('KeyF' === event.code) {
          openSearch();
          return false;
        }
        if ('KeyC' === event.code) {
          copySelection();
          return true;
        }
        if ('KeyV' === event.code) {
          return false;
        }
        return '' !== event.code;
      }
      if (event.altKey && state.searchView) {
        if ('KeyC' === event.code) {
          toggleCaseSensitive();
          return false;
        }
        if ('KeyR' === event.code) {
          toggleRegex();
          return false;
        }
        if ('KeyW' === event.code) {
          toggleWholeWord();
          return false;
        }
      }
    }
    return true;
  });
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
  console.info('终端关闭！');
  try {
    termOption.websocket?.close();
  } catch (error) {
    console.error(error);
  }
  try {
    termOption.term?.dispose();
  } catch (error) {
    console.debug(error);
  }
});
</script>

<style scoped>
.term-main {
  width: 100%;
  min-height: 100px;
  background: #263238;
}

.search-view {
  position: absolute;
  right: 5px;
  top: 2px;
  width: 630px;
  z-index: 999;
  padding: 2px;
  border-radius: 3px;
  background: #263238;
  color: #e9e9eb;
}
.search-view-btn {
  position: absolute;
  right: 15px;
  top: 2px;
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
