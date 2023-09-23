<script setup lang="ts">
import type Node from 'element-plus/es/components/tree/src/model/node';
import StringUtil from '@/common/StringUtil';
import FileIcon from '@/components/file-icon.vue';
import type { FileNode } from '@/types';
import FileRowTool from '@/components/file-manager/file-row-tool.vue';

const props = defineProps<{
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
  (e: 'upload', node: Node): void;
  (e: 'download', node: Node): void;
  (e: 'add-file', node: Node): void;
  (e: 'add-folder', node: Node): void;
  (e: 'reload', node: Node): void;
  (e: 'edit', node: Node): void;
  (e: 'delete', node: Node): void;
}>();

function showProgress(data: FileNode) {
  if (null == data.progress) {
    return false;
  }
  return data.progress > 0 && data.progress < 100;
}
</script>

<template>
  <div style="width: 100%">
    <div class="node-row">
      <div style="flex: auto; width: 100%">
        <el-dropdown trigger="contextmenu" style="width: 100%" size="small" @command="cmd => emit(cmd, props.node)">
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item icon="UploadFilled" command="upload">{{ $t('UPLOAD_TITLE') }}</el-dropdown-item>
              <el-dropdown-item icon="DocumentAdd" command="add-file">{{ $t('ADD_FILE') }}</el-dropdown-item>
              <el-dropdown-item icon="FolderAdd" command="add-folder">{{ $t('ADD_FOLDER') }}</el-dropdown-item>
              <el-dropdown-item v-if="!data.directory" icon="Download" command="download">{{ $t('DOWNLOAD') }}</el-dropdown-item>
              <el-dropdown-item icon="Delete" style="color: var(--el-color-error)" command="delete">{{ $t('DELETE') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
          <div style="line-height: 24px">
            <file-icon :directory="data.directory" :filename="data.name" class="row-icon-style"></file-icon>
            <el-tooltip :title="data?.name || ''" :width="350" placement="right">
              <span>
                {{ data.name }}
                <span v-if="showProgress(data)">
                  <el-progress :percentage="data.progress" :stroke-width="16" class="upload-progress" striped striped-flow text-inside />
                </span>
              </span>
              <template #content>
                <div>
                  <div>{{ data.name }}</div>
                  <div v-if="data?.directory">{{ $t('COUNT') }}: {{ data.children?.length || 0 }}</div>
                  <div v-else>{{ $t('SIZE') }}: {{ data.size || 0 }}</div>
                  <div>{{ $t('MODIFY_TIME') }}: {{ StringUtil.timeFormat(data.modifyTime || 0) }}</div>
                </div>
              </template>
            </el-tooltip>
          </div>
        </el-dropdown>
      </div>
      <div v-if="rowTools || !data.parent" class="node-row-tool">
        <file-row-tool
          :node="node"
          :data="data"
          :row-tools="rowTools"
          @upload="emit('upload', node)"
          @download="emit('download', node)"
          @edit="emit('edit', node)"
          @delete="emit('delete', node)"
          @add-file="emit('add-file', node)"
          @add-folder="emit('add-folder', node)"
          @reload="emit('reload', node)"></file-row-tool>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.node-row {
  width: 100%;
  display: flex;
  line-height: 24px;
}
.row-icon-style {
  margin-right: 5px;
  position: relative;
  top: 2px;
}
.upload-progress {
  display: inline-block;
  position: relative;
  top: 3px;
  width: 100px;
}
.node-row-tool {
  width: 100px;
  display: flex;
  justify-content: right;
}
</style>
