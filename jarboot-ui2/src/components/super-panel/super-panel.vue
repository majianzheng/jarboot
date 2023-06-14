<template>
  <div class="super-panel" ref="panelRef">
    <div class="super-panel-header" :style="{ width: width + 'px' }">
      <div>
        <el-button @click="$emit('close', props.sid)" size="small" plain link icon="CloseBold" :title="$t('CLOSE')"></el-button>
        <span class="panel-title">{{ props.name }}</span>
      </div>
      <div class="panel-middle-title">
        <span v-if="!!state.executing || !!state.view">
          <el-icon v-if="state.executing" style="position: relative; top: 3px" class="ui-spin"><Loading /></el-icon>
          <span style="margin: 0 6px">{{ middleTitle }}</span>
          <el-button v-if="!!state.executing" link class="tool-button" @click="$emit('cancel')" :title="$t('CANCEL')">
            <template #icon>
              <i class="iconfont icon-stopped tool-button-red-icon"></i>
            </template>
          </el-button>
          <el-button v-else link class="tool-button" @click="state.view = ''" :title="$t('CLOSE')">
            <template #icon>
              <el-icon><CloseBold /></el-icon>
            </template>
          </el-button>
        </span>
      </div>
      <div class="panel-header-tools">
        <div class="tool-button" @click="clearDisplay" :title="$t('CLEAR')">
          <i class="iconfont icon-clear tool-button-red-icon"></i>
        </div>
        <div class="tool-button" :title="$t('SCROLL_TO_TOP')" @click="() => pubsub.publish(props.sid, CONSOLE_TOPIC.SCROLL_TO_TOP)">
          <i class="iconfont icon-to-top"></i>
        </div>
        <div class="tool-button" :title="$t('AUTO_SCROLL_END')" @click="setScrollToEnd" :class="{ active: state.autoScrollEnd }">
          <i class="iconfont icon-to-bottom"></i>
        </div>
        <div class="tool-button" :title="$t('TEXT_WRAP')" :class="{ active: state.textWrap }" @click="state.textWrap = !state.textWrap">
          <i class="iconfont icon-text-wrap"></i>
        </div>
      </div>
    </div>
    <div class="terminal-view" :style="{ width: width + 'px' }" v-show="!state.view">
      <console :id="props.sid" :height="height + 'px'" :pubsub="pubsub" :auto-scroll-end="state.autoScrollEnd" :wrap="state.textWrap">
        <template #content>
          <banner></banner>
        </template>
      </console>
      <el-input
        :disabled="!!state.executing"
        @keydown.native.enter="doExecCommand"
        @keyup.up="historyUp"
        @keyup.down="historyDown"
        size="small"
        :placeholder="$t('COMMAND_PLACEHOLDER')"
        auto-complete="off"
        auto-correct="off"
        auto-capitalize="off"
        spell-check="false"
        v-model="state.command"
        ref="inputRef"
        class="command-input">
        <template #prefix>
          <span>
            <el-icon v-if="state.executing" class="ui-spin"><Loading /></el-icon>
            <el-icon v-else><ArrowRight /></el-icon>
          </span>
        </template>
      </el-input>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'jad'">
      <file-editor v-model="state.data.source" :readonly="true" :name="'xxx.java'" :height="height + 28"></file-editor>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'dashboard'">
      <dashboard-view :data="state.data" :height="height" :width="width"></dashboard-view>
    </div>
    <div :style="{ width: width + 'px', position: 'fixed' }" v-if="state.view === 'heapdump'">堆转储</div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, reactive } from 'vue';
import { pubsub, PUB_TOPIC } from '@/views/services/ServerPubsubImpl';
import { CONSOLE_TOPIC } from '@/types';
import StringUtil from '@/common/StringUtil';
import type { ElInput } from 'element-plus';
import { useBasicStore } from '@/stores';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';

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
};
const props = defineProps<{
  name: string;
  sid: string;
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
} as SuperPanelState);
const MAX_HISTORY = 100;
const historyMap = new Map<string, HistoryProp>();
const inputRef = ref<InstanceType<typeof ElInput>>();
const panelRef = ref<InstanceType<typeof HTMLDivElement>>();
const basic = useBasicStore();
const emit = defineEmits<{
  (e: 'close', sid: string): void;
  (e: 'execute', value: string): void;
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
const historyProp = { cur: 0, history: [] } as HistoryProp;
let lastFocusTime = Date.now();

const focusInput = () => {
  if (Date.now() - lastFocusTime > 1000) {
    inputRef?.value?.focus();
    lastFocusTime = Date.now();
  }
};

const onFocusCommandInput = () => {
  inputRef?.value?.focus();
  inputRef?.value?.select();
};

const onCmdEnd = (msg?: string) => {
  state.executing = null;
  pubsub.publish(props.sid, CONSOLE_TOPIC.FINISH_LOADING, msg);
  onFocusCommandInput();
};
const clearDisplay = () => {
  pubsub.publish(props.sid, CONSOLE_TOPIC.CLEAR_CONSOLE);
  focusInput();
};

const doExecCommand = () => {
  const cmd = state.command;
  if (StringUtil.isEmpty(cmd)) {
    return;
  }
  state.executing = cmd;
  state.view = '';

  pubsub.publish(props.sid, CONSOLE_TOPIC.APPEND_LINE, `<span class="command-prefix">$</span>${cmd}`);
  emit('execute', cmd);
  state.command = '';
  const history = historyProp.history;
  if (history.length > 0 && history[history.length - 1] === cmd) {
    return;
  }
  history.push(cmd);
  if (history.length > MAX_HISTORY) {
    history.shift();
  }
  historyProp.cur = history.length - 1;
};
const historyUp = () => {
  const history = historyProp.history;
  if (historyProp.cur < 0) {
    historyProp.cur = 0;
    return;
  }
  state.command = history[historyProp.cur];
  historyProp.cur--;
};
const historyDown = () => {
  const history = historyProp.history;
  historyProp.cur++;
  if (historyProp.cur >= history.length) {
    historyProp.cur = history.length - 1;
    onFocusCommandInput();
    return;
  }
  state.command = history[historyProp.cur];
};
const setScrollToEnd = () => {
  const autoScrollEnd = !state.autoScrollEnd;
  if (autoScrollEnd) {
    pubsub.publish(props.sid, CONSOLE_TOPIC.SCROLL_TO_END);
  }
  state.autoScrollEnd = autoScrollEnd;
};

const renderView = (resultData: any) => {
  const cmd = resultData.name;
  state.data = resultData;
  state.view = cmd;
  state.executing = cmd;
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
  pubsub.submit(key, PUB_TOPIC.FOCUS_CMD_INPUT, onFocusCommandInput);
  historyMap.set(key, { cur: 0, history: [] } as HistoryProp);
  if (panelRef?.value) {
    panelRef.value.onmouseenter = focusInput;
  }
});
onUnmounted(() => {
  const key = props.sid;
  pubsub.unSubmit(key, PUB_TOPIC.CMD_END, onCmdEnd);
  pubsub.unSubmit(key, PUB_TOPIC.RENDER_JSON, renderView);
  pubsub.unSubmit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
  pubsub.unSubmit(key, PUB_TOPIC.FOCUS_CMD_INPUT, onFocusCommandInput);
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
