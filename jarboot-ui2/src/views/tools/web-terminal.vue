<template>
  <div>
    <el-tabs
      v-if="state.terms.length > 0"
      v-model="state.active"
      type="card"
      editable
      :style="{ width: state.width + 'px' }"
      @tab-change="name => (state.active = name)"
      @edit="editTab">
      <el-tab-pane v-for="(term, i) in state.terms" :key="i" :label="term.name" :name="i">
        <terminal
          :width="state.width"
          :height="state.height"
          :host="term.host"
          @connected="option => onConnected(option, term)"
          @disconnected="() => onDisconnected(term)"></terminal>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else>
      <el-button type="primary" @click.stop="editTab(0, 'add')">{{ $t('CREATE_TERM') }}</el-button>
    </el-empty>
    <el-dialog v-model="state.dialog" :title="$t('CREATE_TERM')" width="300px">
      <el-select v-model="state.selectHost" style="width: 100%">
        <el-option v-for="(host, i) in state.clusterHosts" :key="i" :label="host.name" :value="host.host"></el-option>
      </el-select>
      <template #footer>
        <el-button @click="state.dialog = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" plain @click="connect">{{ $t('CONNECT') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { useBasicStore } from '@/stores';
import { onMounted, nextTick, reactive, watch } from 'vue';
import { debounce } from 'lodash';
import CommonNotice from '@/common/CommonNotice';
import ClusterManager from '@/services/ClusterManager';
import type { TabPaneName } from 'element-plus';

const basicStore = useBasicStore();

interface TermOptions {
  host: string;
  name: string;
  disconnected: boolean;
  termOpt: any;
}

const state = reactive({
  width: basicStore.innerWidth - 80,
  height: basicStore.innerHeight - 90,
  terms: [] as TermOptions[],
  clusterHosts: [] as {host: string, name: string}[],
  selectHost: '',
  active: 0,
  dialog: false,
});

watch(() => [basicStore.innerHeight, basicStore.innerWidth], debounce(resize, 1500, { maxWait: 3000 }));
watch(
  () => state.active,
  newValue => {
    if (newValue < 0) {
      return;
    }
    const term = state.terms[newValue];
    if (!term?.termOpt?.term) {
      return;
    }
    nextTick(() => term.termOpt.term?.focus());
  }
);
function connect() {
  if (state.selectHost) {
    connectTo(state.selectHost);
    state.dialog = false;
  }
}

function connectTo(host: string) {
  const term = {
    name: 'Terminal',
    disconnected: false,
    termOpt: {} as any,
    host,
  };
  state.terms.push(term);
  state.active = state.terms.length - 1;
}

function editTab(name: TabPaneName | undefined, action: 'remove' | 'add') {
  if ('remove' === action) {
    let index = name as number;
    const active = index + 1 >= state.terms.length ? index - 1 : index;
    state.terms.splice(index, 1);
    state.active = active;
  } else {
    if (state.clusterHosts?.length) {
      state.dialog = true;
      return;
    }
    connectTo('');
  }
}

function onConnected(termOption: any, opt: TermOptions) {
  opt.disconnected = false;
  opt.termOpt = termOption;
}
function onDisconnected(opt: TermOptions) {
  opt.disconnected = true;
  CommonNotice.error('Terminal disconnected!');
}

function resize() {
  state.width = basicStore.innerWidth - 80;
  state.height = basicStore.innerHeight - 90;
}
onMounted(async () => {
  state.clusterHosts = await ClusterManager.getOnlineClusterHosts();
});
</script>

<style scoped></style>
