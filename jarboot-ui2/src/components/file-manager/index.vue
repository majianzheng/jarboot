<script setup lang="ts">
import { ElMessageBox, ElTree } from 'element-plus';
import FileService from '@/services/FileService';
import { onMounted, reactive, ref } from 'vue';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { canEdit } from '@/components/editor/LangUtils';
import StringUtil from '@/common/StringUtil';
import type { FileNode } from '@/types';
import type Node from 'element-plus/es/components/tree/src/model/node';
import FileRow from '@/components/file-manager/file-row.vue';
import { useUploadStore } from '@/stores';

const props = defineProps<{
  baseDir: string;
  withRoot: boolean;
  clusterHost?: string;
  clusterHostName?: string;
  showClusterHostInRoot?: boolean;
  rowTools?: {
    download?: boolean;
    edit?: boolean;
    delete?: boolean;
    upload?: boolean;
    addFile?: boolean;
    addFolder?: boolean;
  };
}>();

interface Tree {
  name: string;
  leaf?: boolean;
}

const defaultProps = {
  children: 'children',
  label: 'name',
  isLeaf: 'leaf',
};
const emit = defineEmits<{
  (e: 'edit', path: string, content: string, clusterHost: string): void;
  (e: 'select', file: FileNode, clusterHost: string): void;
  (e: 'node-click', file: FileNode, path: string, clusterHost: string): void;
  (e: 'before-load', clusterHost: string): void;
  (e: 'after-load', data: FileNode[], clusterHost: string): void;
}>();

const uploadStore = useUploadStore();

const state = reactive({
  loading: false,
  dialog: false,
  isNew: false,
  file: { path: '', content: '' },
  data: [] as FileNode[],
});
const treeRef = ref<InstanceType<typeof ElTree>>();
async function reload() {
  state.loading = true;
  try {
    emit('before-load', props.clusterHost || '');
    state.data = await FileService.getFiles(props.baseDir, props.withRoot, props.clusterHost || '');
    if (props.showClusterHostInRoot && props.withRoot && props.clusterHost) {
      state.data[0].name = props.clusterHostName || props.clusterHost;
    }
    emit('after-load', { ...state.data }, props.clusterHost || '');
  } catch (e) {
    if (props.showClusterHostInRoot && props.withRoot && props.clusterHost) {
      state.data[0].name = `${props.clusterHostName} (${CommonUtils.translate('OFFLINE')})`;
    }
  } finally {
    state.loading = false;
  }
}

const loadNode = (node: Node, resolve: (data: Tree[]) => void) => {
  if (node.level === 0) {
    return;
  }
  if (node.data.leaf) {
    resolve([]);
    return;
  }
  const path = FileService.parseFilePath(node, props.baseDir);
  FileService.getFiles(path, false, props.clusterHost).then(data => {
    if (data?.length) {
      resolve(data);
    } else {
      node.data.leaf = true;
      resolve([]);
    }
  });
};

function getSelectNode(node: Node): Node {
  let nodeData = treeRef.value?.getCurrentNode() as FileNode;
  if (nodeData) {
    node = treeRef.value?.getNode(nodeData) as Node;
  }
  return node;
}

async function handleEdit(node: Node) {
  node = getSelectNode(node);
  const nodeData = node.data as FileNode;
  if (nodeData.directory) {
    return;
  }
  const path = FileService.parseFilePath(node, props.baseDir);
  if (!canEdit(node.data?.name)) {
    // 非文本文件
    await ElMessageBox.confirm(CommonUtils.translate('NOT_TEXT_FILE'), CommonUtils.translate('WARN'));
  }
  const content = await FileService.getContent(path, props.clusterHost);
  emit('edit', path, content, props.clusterHost || '');
  state.file = { path, content };
  state.isNew = false;
  state.dialog = true;
}

async function handleDelete(node: Node) {
  node = getSelectNode(node);
  const path = FileService.parseFilePath(node, props.baseDir);
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + path + '?', CommonUtils.translate('WARN'), {});
  await FileService.deleteFile(path, props.clusterHost);
  const parent = node.parent;
  node.remove();
  if (parent) {
    treeRef.value?.setCurrentNode(parent);
  }
  CommonNotice.success(`${CommonUtils.translate('SUCCESS')}`);
}

async function addFolder(node: Node) {
  node = getSelectNode(node);
  const nodeData = node.data as FileNode;
  if (!nodeData.directory) {
    // 选择的为文件时
    node = node.parent;
  }
  const name = await ElMessageBox.prompt(CommonUtils.translate('NAME'), CommonUtils.translate('ADD_FOLDER'));
  if (StringUtil.isEmpty(name.value)) {
    CommonNotice.warn('Name is empty!');
    return;
  }
  const file = FileService.parseFilePath(node, props.baseDir) + '/' + name.value;
  const key = await FileService.addDirectory(file, props.clusterHost);
  await reload();
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  treeRef.value?.setCurrentKey(key);
}

async function addFile(node: Node) {
  node = getSelectNode(node);
  const nodeData = node.data as FileNode;
  if (!nodeData.directory) {
    // 选择的为文件时
    node = node.parent;
  }
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
  let key;
  if (state.isNew) {
    key = await FileService.newFile(state.file.path, state.file.content, props.clusterHost);
    await reload();
  } else {
    key = await FileService.writeFile(state.file.path, state.file.content, props.clusterHost);
  }
  state.dialog = false;
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  treeRef.value?.setCurrentKey(key);
}

function resetForm() {
  state.file = { path: '', content: '' };
}

function download(node: any) {
  node = getSelectNode(node);
  const nodeData = node.data as FileNode;
  if (nodeData.directory) {
    return;
  }
  const path = FileService.parseFilePath(node, props.baseDir);
  FileService.download(
    path,
    node.data.name,
    (result, msg) => {
      if (result) {
        CommonNotice.success(CommonUtils.translate('SUCCESS'));
      } else {
        CommonNotice.error(msg);
      }
    },
    props.clusterHost
  );
}

function upload(node: Node) {
  node = getSelectNode(node);
  const nodeData = node.data as FileNode;
  if (!nodeData.directory) {
    // 选择的为文件时
    node = node.parent as Node;
  }

  const path = FileService.parseFilePath(node, props.baseDir);
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '*/*';
  input.onchange = async () => {
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    const clusterHost = props.clusterHost || '';
    await uploadStore.upload(file, 'workspace', '', path, clusterHost, fileInfo => uploadFinished(node, fileInfo.filename));
  };
  input.dispatchEvent(new MouseEvent('click'));
  input.remove();
}

function uploadFinished(node: Node, filename: string) {
  const data = node.data as FileNode;
  const found = data.children?.find(c => c.name === filename);
  if (!found) {
    let dirs = [node.data.name];
    let parent = node.parent;
    let rootKey = node.data.key;
    while (parent?.data?.name) {
      rootKey = parent.data.key;
      dirs.push(parent.data.name);
      parent = parent.parent;
    }
    dirs.pop();
    reload().then(() => {
      let currentNode = treeRef.value?.getNode(rootKey) as Node;
      expandFind(currentNode, dirs, filename);
    });
    CommonNotice.success(`${CommonUtils.translate('UPLOAD_TITLE')} ${filename} ${CommonUtils.translate('SUCCESS')}`);
  }
}

function expandFind(node: Node, dirs: string[], filename: string) {
  if (!node) {
    return;
  }
  const dirName = dirs.pop();
  if (!dirName) {
    node.expand(() => {
      const found = node.childNodes?.find((c: any) => c.data.name === filename) as Node;
      treeRef.value?.setCurrentNode(found);
    });
    return;
  }
  node.expand(() => {
    const found = node.childNodes?.find((c: any) => c.data.name === dirName) as Node;
    expandFind(found, dirs, filename);
  });
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

function nodeClick(data: FileNode) {
  const node = treeRef.value?.getNode(data);
  const path = FileService.parseFilePath(node, props.baseDir);
  emit('node-click', data, path, props.clusterHost || '');
}

function disableContextMenu(event: PointerEvent) {
  event.preventDefault();
}

defineExpose({
  reload,
  filter,
  getCurrentNode,
});

onMounted(reload);
</script>

<template>
  <div style="width: 100%" v-loading="state.loading" @contextmenu="disableContextMenu">
    <el-tree
      ref="treeRef"
      v-model:data="state.data"
      :props="defaultProps"
      :default-expand-all="false"
      :expand-on-click-node="false"
      :load="loadNode"
      lazy
      :filter-node-method="filterService"
      @current-change="data => emit('select', data, props.clusterHost || '')"
      @node-click="nodeClick"
      node-key="key"
      highlight-current>
      <template #default="{ node, data }">
        <file-row
          :node="node"
          :data="data"
          :row-tools="rowTools"
          @upload="upload"
          @download="download"
          @edit="handleEdit"
          @delete="handleDelete"
          @add-file="addFile"
          @add-folder="addFolder"
          @reload="reload"></file-row>
      </template>
    </el-tree>
    <el-dialog
      :title="(state.isNew ? $t('CREATE') : $t('MODIFY')) + ' ' + state.file.path"
      v-model="state.dialog"
      @closed="resetForm"
      destroy-on-close>
      <file-editor v-model="state.file.content" height="300px" :name="state.file.path"></file-editor>
      <template #footer>
        <el-button @click="state.dialog = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" @click="saveFile">{{ $t('SAVE') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
