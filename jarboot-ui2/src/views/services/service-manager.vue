<template>
  <div v-loading="serviceStore.loading" class="__container-wrapper server-mgr">
    <two-sides-pro
      :show-header="false"
      :left-width="LEFT_SIDE_WIDTH + 'px'"
      :body-height="basic.innerHeight - 50 + 'px'"
      v-model:collapsed="serviceState.collapsed">
      <template #left-content>
        <div class="server-side">
          <service-toolbar
            :activated="serviceState.activated"
            :last-clicked-node="serviceState.lastClickedNode"
            :current-node="serviceState.currentNode"
            @new-service="newService"
            @dashboard="doDashboardCmd"></service-toolbar>
          <div style="flex: auto; padding: 3px 1px">
            <el-input v-model="serviceState.search" placeholder="" prefix-icon="Search" size="small" clearable />
            <div class="tree-container">
              <el-tree
                  ref="treeRef"
                  :data="treeData"
                  :props="defaultProps"
                  default-expand-all
                  highlight-current
                  @current-change="data => serviceState.selectClusterHost = data.host"
                  :filter-node-method="filterService">
                <template #default="{ node, data }">
                  <instance-tree-item
                      :node="node"
                      :data="data"
                      @row-click="currentChange"
                      @edit="editService"
                      @select="onSelectClick"
                      :current-node="serviceState.currentNode"
                      :is-service="isService"></instance-tree-item>
                </template>
              </el-tree>
            </div>
          </div>
        </div>
      </template>
      <template #right-content>
        <div class="server-content">
          <super-panel
            v-for="(s, i) in serviceState.activatedList"
            :key="i"
            v-show="serviceState.activated.sid === s.sid"
            :cluster-host="s.host"
            :cluster-host-name="s.hostName"
            :sid="(s.sid as string)"
            :status="s.status"
            @close="closeServiceTerminal(s)"
            @execute="(cmd, cols, rows) => doCommand(s, cmd, cols, rows)"
            @cancel="doCancel(s)"
            :width="getWidth()"
            :name="s.name"></super-panel>
          <el-empty v-if="serviceState.activatedList.length === 0" />
        </div>
      </template>
    </two-sides-pro>
    <service-config
      :show-edit="serviceState.showEdit"
      :is-new="serviceState.isNew"
      :setting="serviceState.configForm"
      :cluster-host="serviceState.selectClusterHost"
      :activated="serviceState.activated"></service-config>
    <el-dialog v-model="serviceState.trustedDialog" :title="$t('WARN')" width="30%" draggable destroy-on-close @closed="onDestroyed">
      <p>{{ serviceState.notTrustedMsg }}</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="serviceState.trustedDialog = false">{{ $t('CANCEL') }}</el-button>
          <el-button type="primary" @click="onTrustOnce">{{ $t('TRUST_ONCE') }}</el-button>
          <el-button type="primary" @click="onTrustAlways">{{ $t('TRUST_ALWAYS') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElTree } from 'element-plus';
import { STATUS_STARTING } from '@/common/CommonConst';
import { PUB_TOPIC, pubsub } from '@/views/services/ServerPubsubImpl';
import { WsManager } from '@/common/WsManager';
import { FuncCode } from '@/common/EventConst';
import { useBasicStore, useServiceStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';
import type { FileNode, MsgData, ServerSetting, ServiceInstance } from '@/types';
import { PAGE_SERVICE } from '@/common/route-name-constants';
import { useRoute } from 'vue-router';
import InstanceTreeItem from '@/views/services/components/instance-tree-item.vue';
import serviceToolbar from '@/views/services/components/service-toolbar.vue';
import ServiceConfig from '@/views/services/components/service-config.vue';

const defaultProps = {
  children: 'children',
  label: 'name',
};

const defaultSetting: ServerSetting = {
  host: '',
  name: '',
  group: '',
  applicationType: 'java',
  args: '',
  scheduleType: 'once',
  cron: '',
  command: '',
  daemon: false,
  env: '',
  fileUpdateWatch: false,
  jdkPath: '',
  lastModified: 0,
  priority: 1,
  sid: '',
  vm: '',
  vmContent: '',
  workDirectory: '',
  workspace: '',
  serviceDir: null as unknown as FileNode,
};

const treeRef = ref<InstanceType<typeof ElTree>>();

const route = useRoute();
const basic = useBasicStore();
const serviceStore = useServiceStore();

const LEFT_SIDE_WIDTH = 380;

const serviceState = reactive({
  isNew: false,
  showEdit: false,
  showVmEdit: false,
  importing: false,
  collapsed: false,
  search: '',
  activatedList: [] as ServiceInstance[],
  activated: {} as ServiceInstance,
  lastClickedNode: {} as ServiceInstance,
  // 当前选中的节点
  currentNode: [] as ServiceInstance[],
  checked: [] as ServiceInstance[],
  configForm: { ...defaultSetting },
  trustedDialog: false,
  notTrustedService: null as ServiceInstance | null,
  notTrustedMsg: '',
  selectClusterHost: '',
});

function getWidth() {
  if (serviceState.collapsed) {
    return basic.innerWidth - 18;
  }
  return basic.innerWidth - LEFT_SIDE_WIDTH - 10;
}

const isService = PAGE_SERVICE === route.name;

const treeData = computed(() => (isService ? serviceStore.groups : serviceStore.jvmGroups));

watch(
  () => serviceState.search,
  (value: string) => treeRef.value?.filter(value)
);

function filterService(value: string, data: ServiceInstance) {
  if (!value) {
    return true;
  }
  if (!data.name) {
    return false;
  }
  return data.name.includes(value);
}

function editService(setting: ServerSetting) {
  serviceState.isNew = false;
  serviceState.configForm = { ...defaultSetting, ...setting };
  serviceState.showEdit = true;
}

function newService() {
  serviceState.isNew = true;
  serviceState.configForm = { ...defaultSetting };
  serviceState.showEdit = true;
}

const doDashboardCmd = () => {
  const service = serviceState.activated;
  service?.sid && doCommand(service, 'dashboard', 1, 1);
};

const doCommand = (service: ServiceInstance, cmd: string, cols: number, rows: number) => {
  WsManager.sendMessage({ host: service.host, service: service.name, sid: service.sid, body: cmd, func: FuncCode.CMD_FUNC, cols, rows });
};

const doCancel = (service: ServiceInstance) => {
  WsManager.callFunc(FuncCode.CANCEL_FUNC, service.sid || '', service.host);
};

const onStatusChange = (data: MsgData) => {
  nextTick(() => setStatus(data.sid, data.body));
};
const reload = () => (isService ? serviceStore.reload() : serviceStore.reloadJvmList());

function currentChange(data: ServiceInstance, node: any, event: PointerEvent) {
  serviceState.lastClickedNode = data;
  if (node.isLeaf && 1 !== data.nodeType) {
    const index = serviceState.activatedList.findIndex(item => item.sid === data.sid);
    if (-1 === index) {
      WsManager.callFunc(FuncCode.ACTIVE_WINDOW, data.sid || '', data.host);
      serviceState.activatedList = [...serviceState.activatedList, data];
    }
    serviceState.activated = data;
  }
  if (event.ctrlKey || event.metaKey) {
    // 多选
    const currentNodes = serviceState.currentNode || [];
    currentNodes.push(data);
    serviceState.currentNode = currentNodes;
  } else {
    serviceState.currentNode = [data];
  }
}

function onSelectClick(checked: boolean, data: ServiceInstance) {
  let currentNodes = serviceState.currentNode?.length ? [...serviceState.currentNode] : [];
  const index = currentNodes.findIndex(item => item.sid === data.sid);
  if (checked) {
    if (index < 0) {
      currentNodes.push(data);
    }
  } else {
    if (index < 0) {
      console.info('not select');
    } else {
      currentNodes = currentNodes.splice(index, 1);
    }
  }
  serviceState.currentNode = currentNodes;
}

function closeServiceTerminal(instance: ServiceInstance) {
  const activatedList = serviceState.activatedList.filter(item => item.sid !== instance.sid);
  let activated = { ...serviceState.activated };
  if (serviceState.activated.sid === instance.sid) {
    if (activatedList.length > 0) {
      activated = activatedList[0];
    } else {
      activated = {} as ServiceInstance;
    }
  }
  serviceState.activated = activated;
  WsManager.callFunc(FuncCode.CLOSE_WINDOW, instance.sid || '', instance.host);
  serviceState.activatedList = activatedList;
}

function setStatus(sid: string, status: string) {
  const service = serviceStore.setStatus(sid, status, isService);
  if (service && STATUS_STARTING === status) {
    serviceState.activated = service;
  }
}

function onDestroyed() {
  serviceState.notTrustedService = null;
  serviceState.notTrustedMsg = '';
}
function onTrustOnce() {
  if (!serviceState.notTrustedService?.remote) {
    return;
  }
  const sid = serviceState.notTrustedService.sid;
  const host = serviceState.notTrustedService.host;
  const remote = serviceState.notTrustedService.remote;
  sid && WsManager.callFunc(FuncCode.TRUST_ONCE_FUNC, sid, host, remote);
  serviceState.trustedDialog = false;
}

function onTrustAlways() {
  if (!serviceState.notTrustedService?.remote) {
    return;
  }
  const sid = serviceState.notTrustedService.sid;
  const host = serviceState.notTrustedService.host;
  const remote = serviceState.notTrustedService.remote;
  sid && WsManager.callFunc(FuncCode.TRUST_ALWAYS_FUNC, sid, host, remote);
  serviceState.trustedDialog = false;
}

function onNotTrusted(data: ServiceInstance) {
  serviceState.notTrustedService = data;
  serviceState.notTrustedMsg = CommonUtils.translate('UNTRUSTED_MODEL_BODY', { host: data.remote });
  serviceState.trustedDialog = true;
}

function onReconnect() {
  if (serviceState.activatedList.length) {
    serviceState.activatedList.forEach(data => {
      WsManager.callFunc(FuncCode.ACTIVE_WINDOW, data.sid || '', data.host);
    });
  }
}

onMounted(() => {
  reload();
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.NOT_TRUSTED, onNotTrusted);
  WsManager.addReconnectSuccessHandler(onReconnect);
});
onUnmounted(() => {
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.NOT_TRUSTED, onNotTrusted);
  WsManager.clearReconnectSuccessHandler();
});
</script>

<style lang="less" scoped>
@import '@/assets/main.less';
.server-mgr {
  width: 100%;
  height: 100%;
  padding: 2px 0;
  .server-side {
    display: flex;
    height: calc(100% - 13px);
    background: var(--side-bg-color);
    .el-tree {
      width: 100%;
      background: var(--side-bg-color);
    }
    .tree-container {
      height: calc(100% - 22px);
      overflow: auto;
    }
  }
  .server-content {
    flex: auto;
  }
}
</style>
