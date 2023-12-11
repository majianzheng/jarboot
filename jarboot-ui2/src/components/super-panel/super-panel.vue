<template>
  <div class="super-panel">
    <div class="super-panel-header" :style="{ width: width + 'px' }">
      <div>
        <el-button circle @click="onClose" size="small" plain link icon="CloseBold" :title="$t('CLOSE')"></el-button>
        <span class="panel-title">{{ panelTitle }}</span>
      </div>
      <div class="panel-middle-title">
        <span v-if="!!state.executing || !!state.view">
          <icon-pro icon="Loading" v-if="state.executing" style="position: relative; top: 3px" class="ui-spin"></icon-pro>
          <span style="margin: 0 6px">{{ middleTitle }}</span>
          <el-button v-if="!!state.executing" link class="tool-button" @click="$emit('cancel')" :title="$t('CANCEL')">
            <template #icon>
              <icon-pro icon="icon-stopped" class="tool-button-red-icon"></icon-pro>
            </template>
          </el-button>
          <el-button v-else link class="tool-button" @click="state.view = ''" :title="$t('CLOSE')">
            <template #icon>
              <icon-pro icon="CloseBold"></icon-pro>
            </template>
          </el-button>
        </span>
      </div>
      <div class="panel-header-tools">
        <div class="tool-button" @click="clearDisplay" :title="$t('CLEAR')">
          <icon-pro icon="icon-clear" class="tool-button-red-icon"></icon-pro>
        </div>
        <div class="tool-button" :title="$t('SCROLL_TO_TOP')" @click="() => pubsub.publish(sid, CONSOLE_TOPIC.SCROLL_TO_TOP)">
          <icon-pro icon="icon-to-top"></icon-pro>
        </div>
        <div class="tool-button" :title="$t('AUTO_SCROLL_END')" @click="setScrollToEnd">
          <icon-pro icon="icon-to-bottom"></icon-pro>
        </div>
      </div>
    </div>
    <div class="terminal-view" :style="{ width: width + 'px' }" v-show="!state.view">
      <j-console
        :height="height + 22"
        :id="sid"
        :width="width"
        :executing="state.executing"
        @ready="onReady"
        @command="onCommand"
        @cancel="emit('cancel')"
        @up="historyUp"
        @down="historyDown"
        :pubsub="pubsub"></j-console>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'jad'">
      <file-editor v-model="state.data.source" :readonly="true" :name="'xxx.java'" :height="height + 28"></file-editor>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'dashboard'">
      <dashboard-view :data="state.data" :height="height" :width="width"></dashboard-view>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'heapdump'">
      <heapdump-view :data="state.data" :remote="remote || ''" :cluster-host="clusterHost"></heapdump-view>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, watch } from 'vue';
import { pubsub, PUB_TOPIC } from '@/views/services/ServerPubsubImpl';
import { CONSOLE_TOPIC } from '@/types';
import StringUtil from '@/common/StringUtil';
import { useBasicStore } from '@/stores';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';
import type { Terminal } from 'xterm';
import { EXITED, STATUS_STOPPED, STATUS_STOPPING } from '@/common/CommonConst';

/**
 * 执行记录，上下键
 */
type HistoryProp = {
  /** 当前的游标 */
  cur: number;
  /** 历史记录存储 */
  history: string[];
};
type SuperPanelState = {
  view: string;
  executing: string | null;
  command: string;
  data: any;
  textWrap: boolean;
  autoScrollEnd: boolean;
  historyProp: HistoryProp;
};
const props = defineProps<{
  clusterHost: string | null;
  clusterHostName?: string | null;
  name: string;
  sid: string;
  status: string;
  remote?: string;
  width: number;
}>();
const state = reactive({
  view: '',
  executing: null,
  command: '',
  data: {},
  textWrap: false,
  autoScrollEnd: true,
  historyProp: { cur: 0, history: [] },
} as SuperPanelState);
const MAX_HISTORY = 100;
const basic = useBasicStore();
const emit = defineEmits<{
  (e: 'close', sid: string): void;
  (e: 'execute', value: string, cols: number, rows: number): void;
  (e: 'cancel'): void;
}>();
const height = computed(() => basic.innerHeight - 110);
const middleTitle = computed(() => {
  if (state.view) {
    if ('jad' === state.view) {
      let cls = state.data.classInfo.name as string;
      const index = cls.lastIndexOf('.');
      if (index > 0) {
        cls = cls.substring(index + 1);
      }
      return cls + '.class';
    }
    return state.view;
  }
  return state.executing || '';
});
const panelTitle = computed(() => {
  if (props.clusterHostName) {
    return `${props.clusterHostName} / ${props.name}`;
  }
  return props.name;
});

let term: Terminal | any;

watch(() => props.status, cancelCmd);
function cancelCmd(newStatus: string) {
  if (state.executing && [STATUS_STOPPED, STATUS_STOPPING, EXITED].includes(newStatus)) {
    onCmdEnd();
  }
}

function onClose() {
  state.executing && emit('cancel');
  emit('close', props.sid);
}
function onReady(t: Terminal) {
  term = t;
}

function onCommand(cmd: string) {
  state.command = cmd;
  doExecCommand();
}

const onCmdEnd = (msg?: string) => {
  state.executing = null;
  // term.options.disableStdin = false;
  pubsub.publish(props.sid, CONSOLE_TOPIC.FINISH_LOADING, msg);
  msg && term.writeln(msg);
  term.prompt();
};
const clearDisplay = () => {
  pubsub.publish(props.sid, CONSOLE_TOPIC.CLEAR_CONSOLE);
};

const doExecCommand = () => {
  const cmd = state.command;
  // term.options.disableStdin = !!cmd;
  if (StringUtil.isEmpty(cmd)) {
    return;
  }
  state.executing = cmd;
  state.view = '';

  emit('execute', cmd, term.cols, term.rows);
  state.command = '';
  const history = state.historyProp.history;
  if (history.length > 0 && history[history.length - 1] === cmd) {
    return;
  }
  history.push(cmd);
  if (history.length > MAX_HISTORY) {
    history.shift();
  }
  state.historyProp.cur = history.length - 1;
};
const historyUp = () => {
  const history = state.historyProp.history;
  if (state.historyProp.cur < 0) {
    state.historyProp.cur = 0;
    return;
  }
  const command = history[state.historyProp.cur];
  term.setCurrent(command || '');
  state.historyProp.cur--;
};
const historyDown = () => {
  const history = state.historyProp.history;
  state.historyProp.cur++;
  if (state.historyProp.cur >= history.length) {
    state.historyProp.cur = history.length - 1;
    return;
  }
  const command = history[state.historyProp.cur];
  term.setCurrent(command || '');
};
const setScrollToEnd = () => {
  const autoScrollEnd = !state.autoScrollEnd;
  pubsub.publish(props.sid, CONSOLE_TOPIC.SCROLL_TO_END);
  state.autoScrollEnd = autoScrollEnd;
};

const renderView = (resultData: any) => {
  const cmd = resultData.name;
  state.data = resultData;
  state.view = cmd;
  state.executing = cmd;
  //term.options.disableStdin = !!cmd;
};

const onExecQuickCmd = (cmd: string) => {
  if (state.executing) {
    CommonNotice.info(CommonUtils.translate('COMMAND_RUNNING', { command: state.executing }));
    return;
  }
  if (StringUtil.isEmpty(cmd)) {
    return;
  }
  state.command = cmd;
  doExecCommand();
};

onMounted(() => {
  const key = props.sid;
  pubsub.submit(key, PUB_TOPIC.CMD_END, onCmdEnd);
  pubsub.submit(key, PUB_TOPIC.RENDER_JSON, renderView);
  pubsub.submit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
});
onUnmounted(() => {
  const key = props.sid;
  pubsub.unSubmit(key, PUB_TOPIC.CMD_END, onCmdEnd);
  pubsub.unSubmit(key, PUB_TOPIC.RENDER_JSON, renderView);
  pubsub.unSubmit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
});
</script>

<style lang="less">
@import '@/assets/main.less';
.super-panel {
  position: absolute;
  margin-top: 3px;
  height: 100%;
  width: 100%;
  .super-panel-header {
    height: 24px;
    display: flex;
    border: var(--el-border);
    border-bottom: none;
    background: var(--panel-header-bg-color);
    .close-btn {
      background: #fc605b;
      color: #98050c;
      height: 16px;
      width: 16px;
    }
    .panel-title {
      margin-left: 6px;
      font-size: 12px;
      font-weight: bold;
      color: var(--el-text-color-primary);
    }
    .panel-middle-title {
      text-align: center;
      flex: auto;
    }
    .panel-header-tools {
      display: flex;
      .tool-button {
        color: var(--el-color-primary);
        margin: 0 3px;
        width: 24px;
        font-size: 19px;
        line-height: 24px;
        text-align: center;
        height: 100%;
        &.active {
          background: var(--tool-button-hover-color);
        }
      }
    }
  }
  .terminal-view {
    display: flex;
    flex-direction: column;
    border: var(--el-border);
    border-top: none;
    .command-input {
      background: @console-background;
      color: @console-color;
      font-family: @console-font-family;
      border: none;
      box-shadow: none;
      border-radius: 0;
      width: 100%;
      .commandRightIcon {
        color: #1890ff;
      }
      .el-input__wrapper,
      input.el-input__inner {
        border: none;
        box-shadow: none;
        background: @console-background;
        color: @console-color;
        font-family: @console-font-family;
      }
    }
  }
}
</style>
