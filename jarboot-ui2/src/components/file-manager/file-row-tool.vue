<script setup lang="ts">
import type { FileNode } from '@/types';
import type Node from 'element-plus/es/components/tree/src/model/node';

defineProps<{
  data: FileNode;
  node: Node;
  rowTools?: {
    download?: boolean;
    edit?: boolean;
    delete?: boolean;
    upload?: boolean;
    addFile?: boolean;
    addFolder?: boolean;
  };
}>();

const emit = defineEmits<{
  (e: 'upload', node: Node, data: FileNode): void;
  (e: 'download', node: Node, data: FileNode): void;
  (e: 'add-file', node: Node, data: FileNode): void;
  (e: 'add-folder', node: Node, data: FileNode): void;
  (e: 'reload', node: Node, data: FileNode): void;
  (e: 'edit', node: Node, data: FileNode): void;
  (e: 'delete', node: Node, data: FileNode): void;
}>();

function isRoot(data: FileNode) {
  return !data.parent && data.directory;
}
</script>

<template>
  <div>
    <span v-if="isRoot(data) || (data.directory && rowTools?.upload)" class="row-btn">
      <el-tooltip :content="$t('UPLOAD_TITLE')">
        <el-button icon="UploadFilled" link type="primary" @click.stop="emit('upload', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data) || (!data.directory && rowTools?.download)" class="row-btn">
      <el-tooltip :content="$t('DOWNLOAD')">
        <el-button icon="Download" link type="primary" @click.stop="emit('download', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data) || (data.directory && rowTools?.addFile)" class="row-btn">
      <el-tooltip :content="$t('ADD_FILE')">
        <el-button icon="DocumentAdd" link type="primary" @click.stop="emit('add-file', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data) || (data.directory && rowTools?.addFolder)" class="row-btn">
      <el-tooltip :content="$t('ADD_FOLDER')">
        <el-button icon="FolderAdd" link type="primary" @click.stop="emit('add-folder', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data)" class="row-btn">
      <el-tooltip :content="$t('REFRESH_BTN')">
        <el-button icon="Refresh" link type="primary" @click.stop="emit('reload', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data) || (!data.directory && rowTools?.edit)" class="row-btn">
      <el-tooltip :content="$t('MODIFY')">
        <el-button icon="Edit" link type="primary" @click.stop="emit('edit', node, data)"></el-button>
      </el-tooltip>
    </span>
    <span v-if="isRoot(data) || rowTools?.delete" class="row-btn">
      <el-tooltip :content="$t('DELETE')">
        <el-button icon="Delete" link type="danger" @click.stop="emit('delete', node, data)"></el-button>
      </el-tooltip>
    </span>
  </div>
</template>

<style scoped lang="less">
.row-btn {
  height: 24px;
  margin-right: 5px;
  &:last-child {
    margin-right: 0;
  }
}
</style>
