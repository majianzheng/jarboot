<script setup lang="ts">
import { STATUS_ATTACHED, STATUS_SCHEDULING, STATUS_STARTED, STATUS_STARTING, STATUS_STOPPED, STATUS_STOPPING } from '@/common/CommonConst';
import type { ServiceInstance, TreeNode } from '@/types';
import { useBasicStore, useServiceStore } from '@/stores';
import { ElMessageBox } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import { WsManager } from '@/common/WsManager';
import { FuncCode } from '@/common/EventConst';
import { computed, reactive } from 'vue';
import ClusterManager from '@/services/ClusterManager';
import { ServerSetting } from '@/types';
const props = defineProps<{
  isService: boolean;
  data: ServiceInstance;
  node: TreeNode;
  currentNode: ServiceInstance[];
}>();
const basic = useBasicStore();
const serviceStore = useServiceStore();

const emit = defineEmits<{
  (e: 'row-click', data: ServiceInstance, node: TreeNode, event: PointerEvent): void;
  (e: 'edit', setting: ServerSetting, instance: ServiceInstance): void;
  (e: 'cancel'): void;
  (e: 'select', selected: boolean, data: ServiceInstance): void;
}>();

const state = reactive({
  editLoading: false,
});

function detach(server: ServiceInstance) {
  console.info('detach', { ...server });
  if (!server.sid) {
    return;
  }
  const sid = server.sid || '';
  if (server.remote) {
    ElMessageBox.confirm(CommonUtils.translate('DETACH_MSG'), CommonUtils.translate('WARN'), { type: 'warning' }).then(() => {
      WsManager.callFunc(FuncCode.DETACH_FUNC, sid, server.host);
    });
  } else {
    WsManager.callFunc(FuncCode.DETACH_FUNC, sid, server.host);
  }
}
function getStatusIcon() {
  if (props.isService) {
    if (STATUS_STOPPED === props.data.status) {
      return { icon: 'SuccessFilled', className: 'status-stopped' };
    }
    if (STATUS_STARTED === props.data.status) {
      return { icon: 'CaretRight', className: 'status-running' };
    }
    if (STATUS_STOPPING === props.data.status) {
      return { icon: 'Loading', className: 'status-stopping ui-spin' };
    }
    if (STATUS_SCHEDULING === props.data.status) {
      return { icon: 'Timer', className: 'status-running ui-blink' };
    }
    if (STATUS_STARTING === props.data.status) {
      return { icon: 'Loading', className: 'status-starting ui-spin' };
    }
    return { icon: 'SuccessFilled', className: 'status-stopped' };
  } else {
    if (props.data.attaching) {
      // 正在attach
      return { icon: 'Loading', className: 'status-starting ui-spin' };
    }
    if (STATUS_ATTACHED === props.data.status) {
      const icon = props.data.remote ? 'icon-remote' : 'icon-debug';
      return { icon, className: 'status-running' };
    }
    return { icon: 'icon-debug', className: 'status-stopped' };
  }
}

const statusIconStyle = computed(getStatusIcon);

const groupIcon = computed(() => {
  if (1 === props.data.nodeType) {
    return basic.host === props.data.host ? 'HomeFilled' : 'Platform';
  }
  if (2 === props.data.nodeType) {
    return 'Folder';
  }
  return '';
});
function editTooltipText() {
  if (props.isService) {
    return CommonUtils.translate('MODIFY');
  }
  if (STATUS_ATTACHED === props.data.status || props.data.attached) {
    return 'Detach';
  }
  return 'Attach';
}
function editIcon() {
  if (props.isService) {
    return 'Edit';
  }
  if (STATUS_ATTACHED === props.data.status || props.data.attached) {
    return 'CircleCloseFilled';
  }
  return 'Connection';
}
async function onEdit() {
  if (props.isService) {
    state.editLoading = true;
    try {
      const setting = await ClusterManager.getServerSetting(props.data);
      emit('edit', setting, props.data);
    } catch (e) {
      console.error(e);
    } finally {
      state.editLoading = false;
    }
    return;
  }
  if (STATUS_ATTACHED === props.data.status || props.data.attached) {
    detach(props.data);
    return;
  }
  serviceStore.attach(props.data.host, props.data.pid);
}
function onSelect(value: boolean) {
  emit('select', value, props.data);
}
function notOnline() {
  return 'OFFLINE' === props.data.status || 'AUTH_FAILED' === props.data.status;
}
function hostTitle() {
  const host = props.data.hostName || 'localhost';
  if (notOnline() && props.data.status) {
    return `${host} (${CommonUtils.translate(props.data.status)})`;
  }
  return host;
}
function onDbClick() {
  if (props.isService) {
    ClusterManager.startService([props.data]);
  } else {
    serviceStore.attach(props.data.host, props.data.pid);
  }
}
</script>

<template>
  <div class="row-instance-wrapper">
    <div class="row-line-text" @click="event => emit('row-click', data, node, event)">
      <span v-if="node.isLeaf && 1 !== data.nodeType" style="width: 100%">
        <el-tooltip :content="$t(props.data.status || '')">
          <icon-pro :icon="statusIconStyle.icon" class="icon-size" :class="statusIconStyle.className"></icon-pro>
        </el-tooltip>
        <span class="__tree-title" :title="data.name" @dblclick="onDbClick">{{ data.name }}</span>
      </span>
      <span v-else>
        <icon-pro v-if="1 === data.nodeType" :icon="groupIcon" class="group-icon" :class="{ dead: notOnline() }"></icon-pro>
        <span v-if="1 === data.nodeType" :title="props.data.host || ''" class="host-node" :class="{ dead: notOnline() }">{{ hostTitle() }}</span>
        <span v-else class="__tree-title">{{ data.name || $t('DEFAULT_GROUP') }}</span>
      </span>
    </div>
    <div @click="event => emit('row-click', data, node, event)" @dblclick="onDbClick" style="flex: auto"></div>
    <div class="right-tool">
      <el-tooltip :content="editTooltipText()" v-if="node.isLeaf && 1 !== data.nodeType">
        <el-button type="primary" link :icon="editIcon()" @click="onEdit" :loading="state.editLoading" class="edit-tool"></el-button>
      </el-tooltip>
      <el-checkbox
        v-if="isService"
        @change="onSelect"
        :model-value="currentNode.map(n => n.sid).includes(data.sid)"
        class="row-checkbox"></el-checkbox>
    </div>
  </div>
</template>

<style scoped lang="less">
@import '@/assets/main.less';
.row-instance-wrapper {
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  display: flex;
  .row-line-text {
    flex: auto;
    max-width: 220px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .status-running {
    color: var(--el-color-success);
  }
  .status-stopped {
    color: var(--el-color-info);
  }
  .status-starting {
    color: var(--el-color-primary);
  }
  .status-stopping {
    color: var(--el-color-danger);
  }
  .icon-size,
  .group-icon {
    font-size: 1.268em;
    position: relative;
    top: 3px;
    margin-right: 6px;
  }
  .group-icon {
    color: @primary-color;
  }
  .attachedStatus,
  .noAttachedStatus {
    font-size: @tree-icon-size;
  }
  .__tree-title,
  .host-node {
    position: relative;
    font-size: 15px;
    line-height: 24px;
  }
  .host-node {
    font-weight: bold;
    color: @primary-color;
  }
  .dead {
    color: gray;
  }
  .right-tool {
    .edit-tool {
      line-height: 24px;
      margin-right: 6px;
      font-size: 1.268em;
      position: relative;
      top: -1px;
    }
    .row-checkbox {
      height: 16px;
      font-size: 1.268em;
      margin: 4px 8px 4px 0;
      position: relative;
      top: 1px;
    }
  }
}
</style>
