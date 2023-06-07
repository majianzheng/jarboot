<script setup lang="ts">
import { ElMessageBox, ElTree, type UploadFile, type UploadProgressEvent } from 'element-plus';
import FileService from '@/services/FileService';
import { onMounted, reactive, ref, watch } from 'vue';
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

watch(() => [props.baseDir, props.withRoot], getData);

function genId(btn: string, data: any) {
  return `${prefix}-${btn}-${data.key}`;
}

async function getData() {
  state.loading = true;
  try {
    state.data = await FileService.getFiles(props.baseDir, props.withRoot);
  } finally {
    state.loading = false;
  }
}

function parseFilePath(node: any) {
  let path = node.data.name;
  let parent = node.parent;
  while (parent?.data.name) {
    path = parent.data.name + '/' + path;
    parent = parent.parent;
  }
  return path;
}

async function handleEdit(node: any) {
  const path = parseFilePath(node);
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
  const path = parseFilePath(node);
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + path + '?', CommonUtils.translate('WARN'), {});
  await FileService.deleteFile(path);
  await getData();
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
    };
    treeRef.value?.append(child, data);
  }
  return child;
}

function handleChange(file: UploadFile, data: FileNode) {
  createNode(file, data);
}

function handleProgress(evt: UploadProgressEvent, file: UploadFile, data: FileNode) {
  let child = createNode(file, data);
  let progress: number | null = round(evt.percent, 2);
  if (progress >= 100) {
    progress = null;
  }
  child.progress = progress;
  //console.info('handleProgress>>', { ...child });
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
  const key = await FileService.addDirectory(parseFilePath(node) + '/' + name.value);
  await getData();
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  treeRef.value?.setCurrentKey(key);
}

async function addFile(node: any) {
  const name = await ElMessageBox.prompt(CommonUtils.translate('NAME'), CommonUtils.translate('ADD_FILE'));
  if (StringUtil.isEmpty(name.value)) {
    CommonNotice.warn('Name is empty!');
    return;
  }
  const path = parseFilePath(node) + '/' + name.value;
  state.file = { path, content: '' };
  state.isNew = true;
  state.dialog = true;
}

async function saveFile() {
  let key = '';
  if (state.isNew) {
    key = await FileService.newFile(state.file.path, state.file.content);
    await getData();
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
  const path = parseFilePath(node);
  FileService.download(path, node.data.name, (result, msg) => {
    if (result) {
      CommonNotice.success(CommonUtils.translate('SUCCESS'));
    } else {
      CommonNotice.error(msg);
    }
  });
}

function clickBtn(btn: string) {
  let data = treeRef.value?.getCurrentNode() as FileNode;
  if (!data) {
    return;
  }
  const dirTool = 'upload' === btn || 'addFile' === btn || 'addFolder' === btn;
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
  const id = genId(btn, data);
  const button = document.getElementById(id);
  if (button) {
    button.click();
  } else {
    console.info('not find button', id);
  }
}

onMounted(getData);
</script>

<template>
  <div style="width: 100%" v-loading="state.loading">
    <div class="file-tool-bar" v-if="headTools">
      <el-button v-if="headTools.refresh" type="primary" icon="Refresh" plain @click="getData">{{ $t('REFRESH_BTN') }}</el-button>
      <el-button v-if="headTools.upload" type="primary" icon="UploadFilled" plain @click="clickBtn('upload')">{{
        $t('UPLOAD_TITLE')
      }}</el-button>
      <el-button v-if="headTools.addFile" type="primary" icon="DocumentAdd" plain @click="clickBtn('addFile')">{{ $t('ADD_FILE') }}</el-button>
      <el-button v-if="headTools.addFolder" type="primary" icon="FolderAdd" plain @click="clickBtn('addFolder')">{{
        $t('ADD_FOLDER')
      }}</el-button>
      <el-button v-if="headTools.delete" type="danger" icon="Delete" plain @click="clickBtn('delete')">{{ $t('DELETE') }}</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="state.data"
      :props="defaultProps"
      :default-expand-all="true"
      :expand-on-click-node="false"
      node-key="key"
      highlight-current>
      <template #default="{ node, data }">
        <div class="node-row">
          <div style="flex: auto">
            <file-icon class="row-icon-style" :filename="data.name" :directory="data.directory"></file-icon>
            <el-popover placement="top-start" :title="data.name" :width="350" trigger="click" :content="data.name">
              <template #reference>
                <span :title="data.name">
                  {{ data.name }}
                  <span v-if="showProgress(data)">
                    <el-progress class="upload-progress" :stroke-width="16" text-inside :percentage="data.progress" striped striped-flow />
                  </span>
                </span>
              </template>
              <template #default>
                <span>size: {{ data.size || 0 }}</span>
              </template>
            </el-popover>
          </div>
          <div class="node-row-tool" v-if="rowTools">
            <el-upload
              v-if="rowTools.upload"
              :action="'/api/jarboot/file-manager/file'"
              :headers="getHeader()"
              :data="{ path: parseFilePath(node) }"
              :on-success="(_resp: any, file: UploadFile) => handleSuccess(file, data)"
              :on-progress="(evt: UploadProgressEvent, file: UploadFile) => handleProgress(evt, file, data)"
              :show-file-list="false"
              class="upload-btn">
              <el-tooltip :content="$t('UPLOAD_TITLE')">
                <el-button v-if="data.directory" link type="primary" icon="UploadFilled" :id="genId('upload', data)"></el-button>
              </el-tooltip>
            </el-upload>
            <el-tooltip v-if="rowTools.download" :content="$t('DOWNLOAD')">
              <el-button
                v-if="!data.directory"
                link
                type="primary"
                icon="Download"
                @click="download(node)"
                :id="genId('download', data)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowTools.addFile" :content="$t('ADD_FILE')">
              <el-button
                v-if="data.directory"
                link
                type="primary"
                icon="DocumentAdd"
                :id="genId('addFile', data)"
                @click="addFile(node)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowTools.addFolder" :content="$t('ADD_FOLDER')">
              <el-button
                v-if="data.directory"
                link
                type="primary"
                icon="FolderAdd"
                :id="genId('addFolder', data)"
                @click="addFolder(node)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowTools.edit" :content="$t('MODIFY')">
              <el-button v-if="!data.directory" link type="primary" icon="Edit" @click="handleEdit(node)" :id="genId('edit', data)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowTools.delete" :content="$t('DELETE')">
              <el-button v-if="data.parent" link type="danger" icon="Delete" @click="handleDelete(node)" :id="genId('delete', data)"></el-button>
            </el-tooltip>
          </div>
        </div>
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
  .node-row-tool {
    width: 100px;
    display: flex;
    justify-content: right;
  }
  .upload-btn {
    height: 24px;
    margin-right: 12px;
  }
}
.upload-progress {
  display: inline-block;
  position: relative;
  top: 3px;
  width: 100px;
}
</style>
