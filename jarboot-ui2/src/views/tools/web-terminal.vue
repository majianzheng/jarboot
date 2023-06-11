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
          @connected="option => onConnected(option, term)"
          @disconnected="() => onDisconnected(term)"></terminal>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else>
      <el-button type="primary" @click.stop="editTab(0, 'add')">{{ $t('CREATE_TERM') }}</el-button>
    </el-empty>
  </div>
</template>

<script lang="ts" setup>
import { useBasicStore } from '@/stores';
import { nextTick, reactive, watch } from 'vue';
import { debounce } from 'lodash';
import CommonNotice from '@/common/CommonNotice';

const basicStore = useBasicStore();

interface TermOptions {
  name: string;
  disconnected: boolean;
  termOpt: any;
}

const state = reactive({
  width: basicStore.innerWidth - 80,
  height: basicStore.innerHeight - 103,
  terms: [] as TermOptions[],
  active: 0,
});

watch(() => [basicStore.innerHeight, basicStore.innerWidth], debounce(resize, 1500, { maxWait: 3000 }));
watch(
  () => state.active,
  newValue => {
    const term = state.terms[newValue];
    if (!term.termOpt?.term) {
      return;
    }
    nextTick(() => term.termOpt.term?.focus());
  }
);

function editTab(index: number, action: 'remove' | 'add') {
  if ('remove' === action) {
    const active = index + 1 >= state.terms.length ? index - 1 : index;
    state.terms.splice(index, 1);
    state.active = active;
  } else {
    const term = {
      name: 'Terminal',
      disconnected: false,
      termOpt: {} as any,
    };
    state.terms.push(term);
    state.active = state.terms.length - 1;
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
  state.height = basicStore.innerHeight - 103;
}
</script>

<style scoped></style>
