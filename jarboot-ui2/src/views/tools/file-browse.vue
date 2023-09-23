<script setup lang="ts">
import { useBasicStore, useUserStore } from '@/stores';
import { onMounted, reactive } from 'vue';
import type { FileNode } from '@/types';
import { canEdit } from '@/components/editor/LangUtils';
import FileService from '@/services/FileService';
import ClusterManager from '@/services/ClusterManager';
import { ElMessageBox } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';

interface FileContent extends FileNode {
  clusterHost: string;
  content: string;
  loading?: boolean;
  modified: boolean;
  path: string;
}

const basicStore = useBasicStore();
const userStore = useUserStore();

const state = reactive({
  files: [] as FileContent[],
  sideWidth: 380,
  collapsed: false,
  active: '',
  loading: false,
  reloading: true,
  clusterHosts: [] as string[],
});

async function editTab(key: string, action: 'remove' | 'add') {
  if ('remove' === action) {
    const index = state.files.findIndex(item => item.key === key);
    if (index < 0) {
      return;
    }
    const file = state.files[index];
    if (file.modified) {
      // 文件已修改，是否保存
      try {
        await ElMessageBox.confirm(CommonUtils.translate('CHANGE_SAVE_TIP'), CommonUtils.translate('WARN'), { type: 'warning' });
        await onSave(file);
      } catch (e) {
        console.debug(e);
      }
    }
    if (1 === state?.files?.length) {
      state.files = [];
      state.active = '';
      return;
    }
    const active = index + 1 >= state.files.length ? state.files[index - 1].key : state.files[index].key;
    state.files.splice(index, 1);
    state.active = active;
  }
}

async function handleSelect(file: FileNode, path: string, clusterHost: string) {
  if (file.directory || !canEdit(file.name)) {
    return;
  }
  if (state.files.findIndex(item => item.key === file.key) < 0) {
    state.loading = true;
    try {
      const content = await FileService.getContent(path, clusterHost);
      state.files.push({ ...file, content, path, modified: false, clusterHost });
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
function onChange(file: FileContent) {
  file.modified = true;
}
async function onSave(file: FileContent) {
  if (file.modified) {
    // 保存文件
    await FileService.writeFile(file.path, file.content, file.clusterHost);
    file.modified = false;
  }
}
onMounted(async () => {
  state.clusterHosts = await ClusterManager.getOnlineClusterHosts();
});
</script>

<template>
  <div v-loading="state.reloading">
    <two-sides-pro
      :show-header="false"
      v-model:collapsed="state.collapsed"
      :body-height="basicStore.innerHeight - 56 + 'px'"
      :left-width="state.sideWidth + 'px'">
      <template #left-content>
        <file-manager
          :with-root="true"
          v-if="!state.clusterHosts?.length"
          :base-dir="userStore.userDir"
          @node-click="handleSelect"
          @before-load="state.reloading = true"
          @after-load="state.reloading = false"></file-manager>
        <file-manager
          :with-root="true"
          v-for="(host, i) in state.clusterHosts"
          :key="i"
          :show-cluster-host-in-root="true"
          :cluster-host="host"
          :base-dir="userStore.userDir"
          @node-click="handleSelect"
          @before-load="state.reloading = true"
          @after-load="state.reloading = false"></file-manager>
      </template>
      <template #right-content>
        <el-tabs
          v-loading="state.loading"
          v-if="state.files.length"
          class="file-browse-tabs"
          v-model="state.active"
          type="card"
          editable
          :style="{ width: getTabWidth() + 'px' }"
          @tab-change="name => (state.active = name)"
          @edit="editTab">
          <el-tab-pane v-for="item in state.files" :key="item.key" :label="item.name" :name="item.key">
            <template #label>
              <span class="custom-tabs-label">
                <el-tooltip :content="$t('SAVE')" v-if="item.modified">
                  <el-button link @click.stop="() => onSave(item)">
                    <template #icon>
                      <el-icon><EditPen /></el-icon>
                    </template>
                  </el-button>
                </el-tooltip>
                <span>{{ item.name }}</span>
              </span>
            </template>
            <file-editor
              v-model="item.content"
              :name="item.name"
              :height="basicStore.innerHeight - 90"
              @save="() => onSave(item)"
              @change="() => onChange(item)"></file-editor>
          </el-tab-pane>
        </el-tabs>
        <div v-else>
          <el-empty></el-empty>
        </div>
      </template>
    </two-sides-pro>
  </div>
</template>

<style lang="less">
.file-browse-tabs .custom-tabs-label .el-icon {
  vertical-align: middle;
}
.file-browse-tabs .custom-tabs-label span {
  vertical-align: middle;
  margin-left: 4px;
}
</style>
