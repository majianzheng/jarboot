<script setup lang="ts">
import { ServiceInstance } from '@/types';
import { useBasicStore, useServiceStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { computed, reactive } from 'vue';
import ClusterManager from '@/services/ClusterManager';
import { STATUS_STOPPED } from '@/common/CommonConst';
import { ElMessageBox } from 'element-plus';
import { PAGE_SERVICE } from '@/common/route-name-constants';
import { useRoute } from 'vue-router';

const props = defineProps<{
  activated: ServiceInstance;
  lastClickedNode: ServiceInstance;
  currentNode: ServiceInstance[];
}>();
const emit = defineEmits<{
  (e: 'new-service'): void;
  (e: 'dashboard'): void;
}>();

const state = reactive({
  importing: false,
  deleting: false,
});
const route = useRoute();
const basic = useBasicStore();
const serviceStore = useServiceStore();
const isService = PAGE_SERVICE === route.name;
const isServiceNode = computed(() => {
  if (!props.lastClickedNode?.sid) {
    return false;
  }
  if (1 === props.lastClickedNode.nodeType || 2 === props.lastClickedNode.nodeType) {
    return false;
  }
  return isService;
});

function getSelected() {
  const services = [] as ServiceInstance[];
  return getSelectLoop(props.currentNode, services);
}

function getSelectLoop(nodes: ServiceInstance[], services: ServiceInstance[]) {
  nodes.forEach((node: ServiceInstance) => {
    if (node?.children && node.children.length > 0) {
      getSelectLoop(node.children, services);
    } else {
      services.push(node);
    }
  });
  return services;
}
function exportServer() {
  if (isServiceNode) {
    CommonUtils.exportServer(props.activated.name, props.lastClickedNode?.host || '');
  }
}

function reload() {
  if (isService) {
    serviceStore.reload();
  } else {
    serviceStore.reloadJvmList();
  }
}

function startServices() {
  ClusterManager.startService(getSelected());
}

function stopServices() {
  ClusterManager.stopService(getSelected());
}

function restartServices() {
  ClusterManager.restartService(getSelected());
}

async function deleteService() {
  const instances = getSelected();
  if (!instances.length) {
    return;
  }
  for (const inst of instances) {
    if (STATUS_STOPPED !== inst.status) {
      CommonNotice.warn(CommonUtils.translate('RUNNING_DELETE_INFO', { name: inst.name }));
      return;
    }
  }
  await ElMessageBox.confirm(CommonUtils.translate('DELETE_INFO'), CommonUtils.translate('WARN'), {});
  state.deleting = true;
  try {
    await ClusterManager.deleteService(instances);
  } finally {
    state.deleting = false;
  }
}

function onImport() {
  if (state.importing) {
    return;
  }
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'application/zip';
  input.onchange = async () => {
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    state.importing = true;
    const message = CommonUtils.translate('START_UPLOAD_INFO', { name: file.name });
    CommonNotice.info(message);
    try {
      await ClusterManager.importService(file, props.lastClickedNode?.host || '');
    } finally {
      state.importing = false;
    }
  };
  input.click();
  input.remove();
}
</script>

<template>
  <div class="common-bar __tool-bar">
    <div v-if="isService" @click="startServices" class="tool-button tool-button-icon">
      <icon-pro icon="CaretRight"></icon-pro>
    </div>
    <div v-if="isService" @click="stopServices" class="tool-button tool-button-icon">
      <icon-pro icon="SwitchButton" class="tool-button-red-icon"></icon-pro>
    </div>
    <div v-if="isService" @click="restartServices" class="tool-button tool-button-icon">
      <icon-pro icon="icon-restart"></icon-pro>
    </div>
    <div @click="reload" class="tool-button tool-button-icon">
      <icon-pro icon="Refresh"></icon-pro>
    </div>
    <div v-if="isService" @click="emit('new-service')" class="tool-button tool-button-icon">
      <icon-pro icon="Plus"></icon-pro>
    </div>
    <div @click="emit('dashboard')" class="tool-button tool-button-red-icon" :class="{ disabled: !activated?.sid }">
      <icon-pro icon="icon-dashboard"></icon-pro>
    </div>
    <div v-if="isService" class="tool-button tool-button-icon" :class="{ disabled: state.importing }" @click="onImport">
      <icon-pro :icon="state.importing ? 'Loading' : 'icon-import'" :class="{ 'ui-spin': state.importing }"></icon-pro>
    </div>
    <div v-if="isService" class="tool-button tool-button-icon" :class="{ disabled: !isServiceNode }" @click="exportServer">
      <icon-pro icon="icon-export"></icon-pro>
    </div>
    <div
      v-if="isService"
      @click="deleteService"
      :class="{ disabled: !isServiceNode || state.deleting }"
      class="tool-button tool-button-red-icon">
      <icon-pro :icon="state.deleting ? 'Loading' : 'Delete'" :class="{ 'ui-spin': state.deleting }"></icon-pro>
    </div>
  </div>
</template>

<style scoped lang="less">
@import '@/assets/main.less';
.__tool-bar {
  min-width: 36px;
  max-width: 36px;
  display: flex;
  padding: 0;
  flex-direction: column;
  .tool-button {
    display: flex;
    flex-direction: column;
    justify-content: center;
    text-align: center;
    .el-icon {
      padding-left: 3px;
    }
    em.iconfont,
    .el-icon {
      font-size: 26px;
    }
    height: 36px;
    width: 100%;
    text-align: center;
    margin-bottom: 5px;
  }
}
</style>
