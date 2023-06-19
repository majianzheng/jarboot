<script setup lang="ts">
import { ElMessageBox, ElTree, type UploadFile, type UploadProgressEvent } from 'element-plus';
import FileService from '@/services/FileService';
import { onMounted, reactive, ref } from 'vue';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { canEdit } from '@/components/editor/LangUtils';
import { useRoute } from 'vue-router';
import StringUtil from '@/common/StringUtil';
import type { FileNode } from '@/types';
import { round } from 'lodash';
import FileIcon from '@/components/file-icon.vue';

const props = defineProps<{
  baseDir: string;
  withRoot: boolean;
  headTools?: {
    refresh?: boolean;
    delete?: boolean;
    upload?: boolean;
    addFile?: boolean;
    addFolder?: boolean;
  };
  rowTools?: {
    download?: boolean;
    edit?: boolean;
    delete?: boolean;
    upload?: boolean;
    addFile?: boolean;
    addFolder?: boolean;
  };
}>();

const defaultProps = {
  children: 'children',
  label: 'name',
};
const emit = defineEmits<{
  (e: 'edit', path: string, content: string): void;
  (e: 'select', file: FileNode): void;
  (e: 'node-click', file: FileNode, path: string): void;
  (e: 'before-load'): void;
  (e: 'after-load', data: FileNode[]): void;
}>();

const state = reactive({
  loading: false,
  dialog: false,
  isNew: false,
  file: { path: '', content: '' },
  data: [] as FileNode[],
});
const route = useRoute();
const treeRef = ref<InstanceType<typeof ElTree>>();
const prefix = `${route.name as string}-${Date.now()}`;

function genId(btn: string, data: any) {
  return `${prefix}-${btn}-${data.key}`;
}

async function reload() {
  state.loading = true;
  try {
    emit('before-load');
    state.data = await FileService.getFiles(props.baseDir, props.withRoot);
    emit('after-load', { ...state.data });
  } finally {
    state.loading = false;
  }
}

async function handleEdit(node: any) {
  const path = FileService.parseFilePath(node, props.baseDir);
  if (!canEdit(node.data?.name)) {
    // 非文本文件
    await ElMessageBox.confirm(CommonUtils.translate('NOT_TEXT_FILE'), CommonUtils.translate('WARN'));
  }
  const content = await FileService.getContent(path);
  emit('edit', path, content);
  state.file = { path, content };
  state.isNew = false;
  state.dialog = true;
}

async function handleDelete(node: any) {
  const path = FileService.parseFilePath(node, props.baseDir);
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + path + '?', CommonUtils.translate('WARN'), {});
  await FileService.deleteFile(path);
  await reload();
  CommonNotice.success(`${CommonUtils.translate('DELETE')}${CommonUtils.translate('SUCCESS')}`);
}

function getHeader() {
  const headers = new Headers();
  headers.set('Authorization', CommonUtils.getToken());
  return headers;
}

function handleSuccess(file: UploadFile, data: FileNode) {
  let child = createNode(file, data);
  child.progress = null;
  treeRef.value?.updateKeyChildren(child.key, { ...child } as any);
  CommonNotice.success(`${CommonUtils.translate('UPLOAD_TITLE')}${file.name}${CommonUtils.translate('SUCCESS')}`);
}

function createNode(file: UploadFile, data: FileNode): FileNode {
  const name = file.name;
  let child = data.children?.find(value => value.name === name);
  if (child) {
    if (null === child.progress || undefined === child.progress) {
      child.progress = 0;
      child.size = file.size;
      treeRef.value?.updateKeyChildren(child.key, { ...child } as any);
    }
  } else {
    const key = '' + file.uid;
    child = {
      key,
      name,
      directory: false,
      parent: data.name,
      progress: 0,
      size: 0,
      modifyTime: file.raw?.lastModified || 0,
    };
    treeRef.value?.append(child, data);
  }
  return child;
}

function handleProgress(evt: UploadProgressEvent, file: UploadFile, data: FileNode) {
  let child = createNode(file, data);
  let progress: number | null = round(evt.percent, 2);
  if (progress >= 100) {
    progress = null;
  }
  child.progress = progress;
  treeRef.value?.updateKeyChildren(child.key, { ...child } as any);
}

function showProgress(data: FileNode) {
  if (null == data.progress) {
    return false;
  }
  return data.progress > 0 && data.progress < 100;
}

async function addFolder(node: any) {
  const name = await ElMessageBox.prompt(CommonUtils.translate('NAME'), CommonUtils.translate('ADD_FOLDER'));
  if (StringUtil.isEmpty(name.value)) {
    CommonNotice.warn('Name is empty!');
    return;
  }
  const key = await FileService.addDirectory(FileService.parseFilePath(node, props.baseDir) + '/' + name.value);
  await reload();
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  treeRef.value?.setCurrentKey(key);
}

async function addFile(node: any) {
  const name = await ElMessageBox.prompt(CommonUtils.translate('NAME'), CommonUtils.translate('ADD_FILE'));
  if (StringUtil.isEmpty(name.value)) {
    CommonNotice.warn('Name is empty!');
    return;
  }
  const path = FileService.parseFilePath(node, props.baseDir) + '/' + name.value;
  state.file = { path, content: '' };
  state.isNew = true;
  state.dialog = true;
}

async function saveFile() {
  let key = '';
  if (state.isNew) {
    key = await FileService.newFile(state.file.path, state.file.content);
    await reload();
  } else {
    key = await FileService.writeFile(state.file.path, state.file.content);
  }
  state.dialog = false;
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  treeRef.value?.setCurrentKey(key);
}

function resetForm() {
  state.file = { path: '', content: '' };
}

function download(node: any) {
  const path = FileService.parseFilePath(node, props.baseDir);
  FileService.download(path, node.data.name, (result, msg) => {
    if (result) {
      CommonNotice.success(CommonUtils.translate('SUCCESS'));
    } else {
      CommonNotice.error(msg);
    }
  });
}

function clickBtn(btn: string, select?: FileNode) {
  let data = (treeRef.value?.getCurrentNode() as FileNode) || select;
  if (!data) {
    return;
  }
  handleCommand(btn, data);
}

function handleCommand(cmd: string, data: FileNode) {
  const dirTool = 'upload' === cmd || 'addFile' === cmd || 'addFolder' === cmd;
  const directory = data.directory;
  if (!directory && dirTool) {
    // 获取父节点
    let node = treeRef.value?.getNode(data)?.parent;
    if (!node) {
      console.info('parent is null', data);
      return;
    }
    data = node.data as FileNode;
  }
  const id = genId(cmd, data);
  const button = document.getElementById(id);
  if (button) {
    button.click();
  } else {
    console.info('not find button', id);
  }
}

function filterService(value: any, data: FileNode) {
  if (!value) {
    return true;
  }
  if (!data.name) {
    return false;
  }
  return data.name.includes(value);
}

function filter(value: string) {
  treeRef.value?.filter(value);
}

function getCurrentNode() {
  return treeRef.value?.getCurrentNode();
}

function isRoot(data: FileNode) {
  return !data.parent && data.directory;
}

function nodeClick(data: FileNode) {
  const node = treeRef.value?.getNode(data);
  const path = FileService.parseFilePath(node, props.baseDir);
  emit('node-click', data, path);
}

defineExpose({
  reload,
  filter,
  getCurrentNode,
});

onMounted(reload);
</script>

<template>
  <div style="width: 100%" v-loading="state.loading">
    <div class="file-tool-bar" v-show="headTools">
      <el-button v-show="headTools?.refresh" type="primary" icon="Refresh" plain @click="reload">{{ $t('REFRESH_BTN') }}</el-button>
      <el-button v-show="headTools?.upload" type="primary" icon="UploadFilled" plain @click="clickBtn('upload')">{{
        $t('UPLOAD_TITLE')
      }}</el-button>
      <el-button v-show="headTools?.addFile" type="primary" icon="DocumentAdd" plain @click="clickBtn('addFile')">{{
        $t('ADD_FILE')
      }}</el-button>
      <el-button v-show="headTools?.addFolder" type="primary" icon="FolderAdd" plain @click="clickBtn('addFolder')">{{
        $t('ADD_FOLDER')
      }}</el-button>
      <el-button v-show="headTools?.delete" type="danger" icon="Delete" plain @click="clickBtn('delete')">{{ $t('DELETE') }}</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="state.data"
      :props="defaultProps"
      :default-expand-all="true"
      :expand-on-click-node="false"
      :filter-node-method="filterService"
      @current-change="data => emit('select', data)"
      @node-click="nodeClick"
      node-key="key"
      highlight-current>
      <template #default="{ node, data }">
        <el-dropdown trigger="contextmenu" size="small" style="width: 100%" @command="cmd => handleCommand(cmd, data)">
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item icon="UploadFilled" command="upload">{{ $t('UPLOAD_TITLE') }}</el-dropdown-item>
              <el-dropdown-item icon="DocumentAdd" command="addFile">{{ $t('ADD_FILE') }}</el-dropdown-item>
              <el-dropdown-item icon="FolderAdd" command="addFolder">{{ $t('ADD_FOLDER') }}</el-dropdown-item>
              <el-dropdown-item v-if="!data.directory" icon="Download" command="download">{{ $t('DOWNLOAD') }}</el-dropdown-item>
              <el-dropdown-item icon="Delete" style="color: var(--el-color-error)" command="delete">{{ $t('DELETE') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
          <div class="node-row">
            <div style="flex: auto">
              <file-icon class="row-icon-style" :filename="data.name" :directory="data.directory"></file-icon>
              <el-tooltip placement="right" :title="data.name" :width="350">
                <span>
                  {{ data.name }}
                  <span v-if="showProgress(data)">
                    <el-progress class="upload-progress" :stroke-width="16" text-inside :percentage="data.progress" striped striped-flow />
                  </span>
                </span>
                <template #content>
                  <div>{{ data.name }}</div>
                  <div v-if="data.directory">file count: {{ data.children.length || 0 }}</div>
                  <div v-else>size: {{ data.size || 0 }}</div>
                  <div>modifyTime: {{ StringUtil.timeFormat(data.modifyTime || 0) }}</div>
                </template>
              </el-tooltip>
            </div>
            <div class="node-row-tool" v-show="rowTools || !data.parent">
              <span v-if="data.directory" v-show="rowTools?.upload && data.parent" class="row-btn">
                <el-upload
                  :action="'/api/jarboot/file-manager/file'"
                  :headers="getHeader()"
                  :data="{ path: FileService.parseFilePath(node, baseDir) }"
                  :on-success="(_resp: any, file: UploadFile) => handleSuccess(file, data)"
                  :on-progress="(evt: UploadProgressEvent, file: UploadFile) => handleProgress(evt, file, data)"
                  :show-file-list="false">
                  <el-tooltip :content="$t('UPLOAD_TITLE')">
                    <el-button link type="primary" icon="UploadFilled" :id="genId('upload', data)"></el-button>
                  </el-tooltip>
                </el-upload>
              </span>
              <span v-if="isRoot(data)" class="row-btn">
                <el-tooltip :content="$t('UPLOAD_TITLE')">
                  <el-button link type="primary" icon="UploadFilled" @click.stop="clickBtn('upload', data)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="!data.directory" v-show="rowTools?.download" class="row-btn">
                <el-tooltip :content="$t('DOWNLOAD')">
                  <el-button link type="primary" icon="Download" @click.stop="download(node)" :id="genId('download', data)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="isRoot(data)" class="row-btn">
                <el-tooltip :content="$t('ADD_FILE')">
                  <el-button link type="primary" icon="DocumentAdd" @click.stop="clickBtn('addFile', data)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="data.directory" v-show="!isRoot(data) && (rowTools?.addFile || !data.parent)" class="row-btn">
                <el-tooltip :content="$t('ADD_FILE')">
                  <el-button link type="primary" icon="DocumentAdd" :id="genId('addFile', data)" @click.stop="addFile(node)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="isRoot(data)" class="row-btn">
                <el-tooltip :content="$t('ADD_FOLDER')">
                  <el-button link type="primary" icon="FolderAdd" @click.stop="clickBtn('addFolder', data)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="data.directory" v-show="!isRoot(data) && (rowTools?.addFolder || !data.parent)" class="row-btn">
                <el-tooltip :content="$t('ADD_FOLDER')">
                  <el-button link type="primary" icon="FolderAdd" :id="genId('addFolder', data)" @click.stop="addFolder(node)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="isRoot(data)" class="row-btn">
                <el-tooltip :content="$t('REFRESH_BTN')">
                  <el-button link type="primary" icon="Refresh" @click.stop="reload"></el-button>
                </el-tooltip>
              </span>
              <span v-if="!data.directory" v-show="rowTools?.edit" class="row-btn">
                <el-tooltip :content="$t('MODIFY')">
                  <el-button link type="primary" icon="Edit" @click.stop="handleEdit(node)" :id="genId('edit', data)"></el-button>
                </el-tooltip>
              </span>
              <span v-if="data.parent" v-show="rowTools?.delete" class="row-btn">
                <el-tooltip :content="$t('DELETE')">
                  <el-button link type="danger" icon="Delete" @click.stop="handleDelete(node)" :id="genId('delete', data)"></el-button>
                </el-tooltip>
              </span>
            </div>
          </div>
        </el-dropdown>
      </template>
    </el-tree>
    <el-dialog
      :title="(state.isNew ? $t('CREATE') : $t('MODIFY')) + ' ' + state.file.path"
      v-model="state.dialog"
      @closed="resetForm"
      destroy-on-close>
      <file-editor v-model="state.file.content" height="500px" :name="state.file.path"></file-editor>
      <template #footer>
        <el-button @click="state.dialog = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" @click="saveFile">{{ $t('SAVE') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="less">
.file-tool-bar {
  margin-bottom: 5px;
  display: flex;
  justify-content: right;
}
.row-icon-style {
  margin-right: 5px;
  position: relative;
  top: 2px;
}
.node-row {
  width: 100%;
  display: flex;
  line-height: 24px;
  .node-row-tool {
    width: 100px;
    display: flex;
    justify-content: right;
  }
  .row-btn {
    height: 24px;
    margin-right: 5px;
    &:last-child {
      margin-right: 0;
    }
  }
}
.upload-progress {
  display: inline-block;
  position: relative;
  top: 3px;
  width: 100px;
}
</style>
