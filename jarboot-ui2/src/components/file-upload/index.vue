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
    <el-popover :visible="uploadStore.visible" width="580" title="上传进度" placement="left-start">
      <template #reference>
        <el-badge :value="uploadStore.uploadFiles.length" class="item">
          <icon-pro title="文件上传" class="upload-icon" icon="UploadFilled" @click="toggleVisible"></icon-pro>
        </el-badge>
      </template>
      <div>
        <el-table :data="uploadStore.uploadFiles" :show-header="false">
          <el-table-column property="name" label="文件名" />
          <el-table-column width="200" property="uploadedSize" label="进度">
            <template #default="{ row }">
              <el-progress text-inside :percentage="calcPercent(row)" :stroke-width="15" striped :striped-flow="row.uploadedSize < row.total" />
            </template>
          </el-table-column>
          <el-table-column width="60">
            <template #default="{ row }">
              <el-button v-if="row.uploadedSize < row.total" type="primary" :icon="row.pause ? 'CaretRight' : 'VideoPause'" link></el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-popover>
  </div>
</template>

<style scoped lang="less">
.file-upload-container {
  z-index: 9999;
  .upload-icon {
    font-size: 32px;
    color: var(--el-color-primary);
  }
}
</style>
