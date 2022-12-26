<template>
  <div v-loading="services.loading" class="__container-wrapper server-mgr">
    <div class="server-side">
      <div class="common-bar __tool-bar">
        <div @click="services.startServices()" class="tool-button tool-button-icon">
          <el-icon class="status-running"><CaretRight/></el-icon>
        </div>
        <div @click="services.stopServices()" class="tool-button tool-button-icon">
          <el-icon class="tool-button-red-icon"><SwitchButton /></el-icon>
        </div>
        <div @click="services.reload()" class="tool-button tool-button-icon">
          <el-icon><Refresh /></el-icon>
        </div>
        <div class="tool-button tool-button-red-icon"><i class="iconfont icon-dashboard"></i> </div>
        <div class="tool-button tool-button-icon"><i class="iconfont icon-import"></i> </div>
      </div>
      <el-tree
          :data="services.groups"
          default-expand-all
          ref="treeRef"
          :props="defaultProps"
          highlight-current
          @check-change="checkChanged"
          @node-click="handleNodeClick">
        <template #default="{ node, data }">
          <div style="width: 100%;">
            <div style="width: 100%;" v-if="node.isLeaf" @click="services.currentChange(data, node)">
              <i v-if="CommonConst.STATUS_STOPPED === data.status" class="iconfont icon-stopped status-stopped"></i>
              <el-icon v-else-if="CommonConst.STATUS_STARTING === data.status" class="status-starting ui-spin"><Loading/></el-icon>
              <el-icon v-else-if="CommonConst.STATUS_STOPPING === data.status" class="status-stopping ui-spin"><Loading/></el-icon>
              <el-icon v-else-if="CommonConst.STATUS_STARTED === data.status" class="status-running"><CaretRight/></el-icon>
              <span class="__tree-title">{{ data.name }}</span>
            </div>
            <div v-else>
              <el-icon class="group-icon"><Menu /></el-icon>
              <span class="__tree-title">{{ data.name || $t('DEFAULT_GROUP') }}</span>
            </div>
          </div>
        </template>
      </el-tree>
    </div>
    <div class="server-content">
      <super-panel
          v-for="service in services.activatedList"
          v-show="services.activated.sid===service.sid"
          :sid="service.sid"
          @close="services.closeServiceTerminal(service)"
          @execute="cmd => doCommand(service, cmd)"
          @cancel="doCancel(service)"
          :name="service.name"></super-panel>
      <el-empty v-if="services.activatedList.length === 0"/>
    </div>
  </div>
</template>
<script setup lang="ts">
import {ref, onMounted, onUnmounted} from 'vue';
import { ElTree } from 'element-plus';
import CommonConst from '@/common/CommonConst';
import {pubsub, PUB_TOPIC} from "@/views/services/ServerPubsubImpl";
import type {MsgData, ServiceInstance} from "@/common/CommonTypes";
import {WsManager} from "@/common/WsManager";
import {FuncCode} from "@/common/EventConst";
import {useServicesStore} from "@/stores";

const defaultProps = {
  children: 'children',
  label: 'name',
};
const treeRef = ref<InstanceType<typeof ElTree>>();

const services = useServicesStore();

const handleNodeClick = (row: ServiceInstance) => {
  console.info('row', row);
};

const checkChanged = () => {
  const nodes = treeRef.value?.getCheckedNodes();
  const checked = nodes?.map(node => ({...node})) || [];
  services.checkChange(checked);
};
const doCommand = (service: ServiceInstance, cmd: string) => {
  WsManager.sendMessage({service: service.name, sid: service.sid, body: cmd, func: FuncCode.CMD_FUNC});
}
const doCancel = (service: ServiceInstance) => {
  WsManager.callFunc(FuncCode.CANCEL_FUNC, service.sid);
}

const onStatusChange = (data: MsgData) => {
  services.setStatus(data.sid, data.body);
};
const reload = () => services.reload();

onMounted(() => {
  services.reload();
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
});
onUnmounted(() => {
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
});
</script>

<style lang="less" scoped>
@import "@/assets/main.less";
.server-mgr {
  display: flex;
  width: 100%;
  height: 100%;
  .server-side {
    width: @side-width;
    display: flex;
    height: 100%;
    background: var(--side-bg-color);
    .el-tree {
      width: 100%;
      background: var(--side-bg-color);
    }
  }
  .__tree-title {
    margin-left: 6px;
  }
  .__tool-bar {
    width: 36px;
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
      i.iconfont, .el-icon {
        font-size: 26px;
      }
      height: 36px;
      width: 100%;
      text-align: center;
      margin-bottom: 5px;
    }
  }
  .server-content {
    flex: auto;
  }
  .status-running {
    color: var(--el-color-success);
  }
  .status-stopped {
    color: var(--el-color-error);
  }
  .status-starting {
    color: var(--el-color-primary);
  }
  .status-stopping {
    color: var(--el-color-danger)
  }
  .attached-status {
    color: green;
  }
  .no-attached-status {
    color: gray;
  }
  .group-icon {
    color: @primary-color;
    font-size: 1.268em;
  }
  .attachedStatus, .noAttachedStatus {
    font-size: @tree-icon-size;
  }
}
</style>
