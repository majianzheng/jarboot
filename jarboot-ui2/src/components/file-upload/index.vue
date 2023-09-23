<script setup lang="ts">
import { useUploadStore } from '@/stores';
import { round } from 'lodash';
import type { UploadFileInfo } from '@/types';

const uploadStore = useUploadStore();
function toggleVisible() {
  uploadStore.visible = !uploadStore.visible;
}
function calcPercent(row: UploadFileInfo) {
  return round((100 * row.uploadedSize) / row.total, 2);
}
</script>

<template>
  <div v-show="uploadStore.uploadFiles.length" class="file-upload-container">
    <el-popover :visible="uploadStore.visible" width="500" title="上传进度" placement="left-start">
      <template #reference>
        <el-badge :value="uploadStore.uploadFiles.length">
          <icon-pro title="文件上传" class="upload-icon" icon="UploadFilled" @click="toggleVisible"></icon-pro>
        </el-badge>
      </template>
      <div>
        <el-table :data="uploadStore.uploadFiles" :show-header="false">
          <el-table-column property="name" width="180" label="文件名" show-overflow-tooltip />
          <el-table-column width="260" property="uploadedSize" label="进度">
            <template #default="{ row }">
              <el-progress text-inside :percentage="calcPercent(row)" :stroke-width="15" striped :striped-flow="row.uploadedSize < row.total" />
            </template>
          </el-table-column>
          <el-table-column width="60" v-if="false">
            <template #default="{ row }">
              <el-tooltip v-if="row.uploadedSize < row.total" :content="$t('REFRESH_BTN')" placement="top">
                <icon-pro :icon="row.pause ? 'CaretRight' : 'VideoPause'" size="18px" class="tool-button tool-button-icon"></icon-pro>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-popover>
  </div>
</template>

<style scoped lang="less">
@import '@/assets/main.less';
.file-upload-container {
  z-index: 9999;
  .upload-icon {
    font-size: 32px;
    border-radius: 16px;
    color: var(--el-color-primary);
    box-shadow: 5px 5px 5px #888888;
    &:hover {
      background: var(--tool-button-hover-color);
      cursor: pointer;
    }
  }
  .tool-button-icon {
    margin: auto 0;
  }
}
</style>
