<script setup lang="ts">
import { useBasicStore, useUserStore } from '@/stores';
import { reactive } from 'vue';
import type { FileNode } from '@/types';
import { canEdit } from '@/components/editor/LangUtils';
import FileService from '@/services/FileService';

interface FileContent extends FileNode {
  content: string;
  loading?: false;
}

const basicStore = useBasicStore();
const userStore = useUserStore();

const state = reactive({
  files: [] as FileContent[],
  sideWidth: 380,
  collapsed: false,
  active: '',
  loading: false,
});

function editTab(key: string, action: 'remove' | 'add') {
  if ('remove' === action) {
    const index = state.files.findIndex(item => item.key === key);
    if (index < 0) {
      return;
    }
    const active = index + 1 >= state.files.length ? state.files[index - 1].key : state.files[index].key;
    state.files.splice(index, 1);
    state.active = active;
  }
}

async function handleSelect(file: FileNode, path: string) {
  if (file.directory || !canEdit(file.name)) {
    return;
  }
  if (state.files.findIndex(item => item.key === file.key) < 0) {
    state.loading = true;
    try {
      const content = await FileService.getContent(path);
      state.files.push({ ...file, content });
    } finally {
      state.loading = false;
    }
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
        <file-manager :with-root="true" :base-dir="userStore.userDir" @node-click="handleSelect"></file-manager>
      </template>
      <template #right-content>
        <el-tabs
          v-loading="state.loading"
          v-if="state.files.length"
          v-model="state.active"
          type="card"
          editable
          :style="{ width: getTabWidth() + 'px' }"
          @tab-change="name => (state.active = name)"
          @edit="editTab">
          <el-tab-pane v-for="item in state.files" :key="item.key" :label="item.name" :name="item.key">
            <file-editor v-model="item.content" :name="item.name" :height="basicStore.innerHeight - 103"></file-editor>
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
