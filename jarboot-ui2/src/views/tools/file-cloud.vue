<script setup lang="ts">
import { useBasicStore } from '@/stores';
import { reactive } from 'vue';
import type { FileNode } from '@/types';

const basicStore = useBasicStore();
const state = reactive({
  files: [] as FileNode[],
  sideWidth: 380,
  collapsed: false,
  active: '',
});

function editTab(key: string, action: 'remove' | 'add') {
  // ingore
  console.error('>>>', key, action);
  if ('remove' === action) {
    const index = state.files.findIndex(item => item.key === key);
    if (index < 0) {
      return;
    }
    state.files.splice(index, 1);
  }
}

function handleSelect(file: FileNode) {
  if (file.directory) {
    return;
  }
  if (state.files.findIndex(item => item.key === file.key) < 0) {
    state.files.push(file);
  }
  state.active = file.key;
}

function getTabWidth() {
  if (state.collapsed) {
    return basicStore.innerWidth - 85;
  }
  return basicStore.innerWidth - state.sideWidth - 85;
}
</script>

<template>
  <div>
    <two-sides-pro
      :show-header="false"
      v-model:collapsed="state.collapsed"
      :body-height="basicStore.innerHeight - 72 + 'px'"
      :left-width="state.sideWidth + 'px'">
      <template #left-content>
        <file-manager :with-root="true" :base-dir="''" @node-click="handleSelect"></file-manager>
      </template>
      <template #right-content>
        <el-tabs
          v-if="state.files.length"
          v-model="state.active"
          type="card"
          editable
          :style="{ width: getTabWidth() + 'px' }"
          @tab-change="name => (state.active = name)"
          @edit="editTab">
          <el-tab-pane v-for="item in state.files" :key="item.key" :label="item.name" :name="item.key">
            {{ item.name }}
          </el-tab-pane>
        </el-tabs>
        <div v-else>
          <el-empty></el-empty>
        </div>
      </template>
    </two-sides-pro>
  </div>
</template>

<style scoped lang="less"></style>
