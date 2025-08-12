<script setup lang="ts">
import { useUploadStore } from '@/stores';
import { round } from 'lodash';
import type { UploadFileInfo } from '@/types';
import StringUtil from '@/common/StringUtil';
import { computed } from 'vue';

const uploadStore = useUploadStore();
function toggleVisible() {
  uploadStore.visible = !uploadStore.visible;
}
function calcPercent(row: UploadFileInfo) {
  if (!row?.totalSize || !row?.uploadSize) {
    return 0;
  }
  return round((100 * row.uploadSize) / row.totalSize, 2) || 0;
}

function uploadSize(row: UploadFileInfo) {
  return `${StringUtil.formatBytes(row.uploadSize).fileSize} / ${StringUtil.formatBytes(row.totalSize).fileSize} `;
}

function pauseOrResume(row: UploadFileInfo) {
  const key = `${row.clusterHost || 'localhost'}://${row.dstPath}`;
  if (row.pause) {
    uploadStore.resume(key);
  } else {
    uploadStore.pause(key);
  }
}
const uploadingCount = computed(() => uploadStore.uploadFiles.filter(row => (row?.uploadSize || 0) < (row?.totalSize || 0)).length);
</script>

<template>
  <div v-if="uploadStore.uploadFiles.length" class="file-upload-container">
    <el-badge :value="uploadingCount" :hidden="uploadingCount <= 0">
      <el-button size="small" link @click="toggleVisible" class="ui-blink">
        <icon-pro title="文件上传" class="upload-icon" icon="UploadFilled"></icon-pro>
      </el-button>
    </el-badge>
    <el-dialog v-model="uploadStore.visible" title="上传进度" width="800" destroy-on-close :modal="false" :draggable="true">
      <div>
        <el-table :data="uploadStore.uploadFiles" :show-header="true">
          <el-table-column property="filename" width="160" :label="$t('FILE_NAME')" show-overflow-tooltip />
          <el-table-column :label="$t('SIZE')">
            <template #default="{ row }">
              <span>{{ uploadSize(row) }}({{ row.uploadSize >= row.totalSize ? $t('FINISHED') : $t('TRANSMITTING') }})</span>
            </template>
          </el-table-column>
          <el-table-column width="260" property="uploadSize" :label="$t('STATUS')">
            <template #default="{ row }">
              <el-progress
                text-inside
                :percentage="calcPercent(row)"
                :stroke-width="15"
                striped
                :striped-flow="row.uploadSize < row.totalSize" />
            </template>
          </el-table-column>
          <el-table-column width="60" :label="$t('OPERATOR')">
            <template #default="{ row }">
              <el-button v-if="row.uploadSize < row.totalSize" link @click="pauseOrResume(row)">
                <icon-pro :icon="row.pause ? 'CaretRight' : 'VideoPause'" size="18px" class="tool-button tool-button-icon"></icon-pro>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="less">
@import '@/assets/main.less';
.file-upload-container {
  .upload-icon {
    font-size: var(--el-font-size-extra-large);
    color: var(--el-color-primary);
    &:hover {
      cursor: pointer;
    }
  }
  .tool-button-icon {
    margin: auto 0;
  }
}
</style>
