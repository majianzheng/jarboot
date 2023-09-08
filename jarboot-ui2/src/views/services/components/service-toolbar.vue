<script setup lang="ts">
import { ServiceInstance } from '@/types';
import { useBasicStore, useServiceStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { reactive } from 'vue';
import ClusterManager from '@/services/ClusterManager';

const props = defineProps<{
  isService: boolean;
  activated: ServiceInstance;
}>();
const emit = defineEmits<{
  (e: 'start'): void;
  (e: 'stop'): void;
  (e: 'new-service'): void;
  (e: 'dashboard'): void;
}>();

const state = reactive({
  importing: false,
});
const basic = useBasicStore();
const serviceStore = useServiceStore();

function exportServer() {
  if (props.activated.name) {
    if (props.activated.pid) {
      return;
    }
    CommonUtils.exportServer(props.activated.name, props.activated?.host || '');
  }
}

function reload() {
  if (props.isService) {
    serviceStore.reload();
  } else {
    serviceStore.reloadJvmList();
  }
}

function onImport() {
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
      console.info('>>>>', props.activated);
      await ClusterManager.importService(file, props.activated?.host || '');
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
    <div v-if="isService" @click="emit('start')" class="tool-button tool-button-icon">
      <icon-pro icon="CaretRight"></icon-pro>
    </div>
    <div v-if="isService" @click="emit('stop')" class="tool-button tool-button-icon">
      <icon-pro icon="SwitchButton" class="tool-button-red-icon"></icon-pro>
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
    <div v-if="isService" class="tool-button tool-button-icon" :class="{ disabled: !activated?.sid || activated?.pid }" @click="exportServer">
      <icon-pro icon="icon-export"></icon-pro>
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
