<template>
  <div v-loading="serviceStore.loading" class="__container-wrapper server-mgr">
    <two-sides-pro
      :show-header="false"
      :left-width="LEFT_SIDE_WIDTH + 'px'"
      :body-height="basic.innerHeight - 50 + 'px'"
      v-model:collapsed="serviceState.collapsed">
      <template #left-content>
        <div class="server-side">
          <div class="common-bar __tool-bar">
            <div v-if="isService" @click="startServices" class="tool-button tool-button-icon">
              <icon-pro icon="CaretRight"></icon-pro>
            </div>
            <div v-if="isService" @click="stopServices" class="tool-button tool-button-icon">
              <icon-pro icon="SwitchButton" class="tool-button-red-icon"></icon-pro>
            </div>
            <div @click="serviceStore.reload()" class="tool-button tool-button-icon">
              <icon-pro icon="Refresh"></icon-pro>
            </div>
            <div v-if="isService" @click="newService" class="tool-button tool-button-icon">
              <icon-pro icon="Plus"></icon-pro>
            </div>
            <div @click="doDashboardCmd" class="tool-button tool-button-red-icon" :class="{ disabled: !serviceState.activated?.sid }">
              <icon-pro icon="icon-dashboard"></icon-pro>
            </div>
            <div v-if="isService" class="tool-button tool-button-icon" :class="{ disabled: serviceState.importing }" @click="onImport">
              <icon-pro :icon="serviceState.importing ? 'Loading' : 'icon-import'" :class="{ 'ui-spin': serviceState.importing }"></icon-pro>
            </div>
            <div
              v-if="isService"
              class="tool-button tool-button-icon"
              :class="{ disabled: !serviceState.activated?.sid || serviceState.activated?.pid }"
              @click="exportServer">
              <icon-pro icon="icon-export"></icon-pro>
            </div>
          </div>
          <div style="flex: auto; padding: 3px 1px">
            <el-input v-model="serviceState.search" placeholder="" prefix-icon="Search" size="small" clearable />
            <el-tree
              ref="treeRef"
              :data="treeData"
              :props="defaultProps"
              default-expand-all
              highlight-current
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
      </template>
      <template #right-content>
        <div class="server-content">
          <super-panel
            v-for="(s, i) in serviceState.activatedList"
            :key="i"
            v-show="serviceState.activated.sid === s.sid"
            :sid="(s.sid as string)"
            @close="closeServiceTerminal(s)"
            @execute="(cmd, cols, rows) => doCommand(s, cmd, cols, rows)"
            @cancel="doCancel(s)"
            :width="getWidth()"
            :name="s.name"></super-panel>
          <el-empty v-if="serviceState.activatedList.length === 0" />
        </div>
      </template>
    </two-sides-pro>
    <el-drawer
      destroy-on-close
      size="50%"
      :title="serviceState.isNew ? $t('CREATE') : $t('MODIFY')"
      @close="onCloseEdit"
      v-model="serviceState.showEdit">
      <el-form
        ref="configRef"
        size="small"
        :rules="rules"
        :model="serviceState.configForm"
        label-width="auto"
        label-position="right"
        status-icon>
        <el-form-item :label="$t('NAME')" prop="name">
          <el-input v-model="serviceState.configForm.name" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
        </el-form-item>
        <el-form-item :label="$t('GROUP')" prop="group">
          <el-input v-model="serviceState.configForm.group" :placeholder="$t('GROUP_PLACEHOLDER')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('APP_TYPE')" prop="applicationType">
          <el-radio-group v-model="serviceState.configForm.applicationType">
            <el-radio label="java">Java</el-radio>
            <el-radio label="shell">Shell</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('COMMAND_LABEL')" prop="command">
          <el-input
            type="textarea"
            v-model="serviceState.configForm.command"
            :placeholder="
              'java' === serviceState.configForm.applicationType ? $t('COMMAND_EXAMPLE') : 'eg: 1) sh xxx.sh 2) ./app.exe 3) echo Hello!'
            "></el-input>
        </el-form-item>
        <el-form-item :label="$t('VM_OPT_LABEL')" prop="vm">
          <el-input v-model="serviceState.configForm.vm" spell-check="false">
            <template #append>
              <span>
                <el-button v-if="serviceState.showVmEdit" @click.stop="serviceState.showVmEdit = false">{{ $t('CLOSE') }}</el-button>
                <el-button v-else link @click.stop="serviceState.showVmEdit = true">{{ $t('MODIFY') }}</el-button>
              </span>
            </template>
          </el-input>
          <div v-if="serviceState.showVmEdit" style="width: 100%">
            <file-editor
              v-model="serviceState.configForm.vmContent"
              height="200px"
              :name="serviceState.configForm.vm"
              mode="text/x-properties"></file-editor>
          </div>
        </el-form-item>
        <el-form-item :label="$t('MAIN_ARGS_LABEL')" prop="args">
          <el-input
            placeholder="Main arguments"
            auto-complete="off"
            auto-correct="off"
            auto-capitalize="off"
            v-model="serviceState.configForm.args"></el-input>
        </el-form-item>
        <el-form-item v-show="'java' === serviceState.configForm.applicationType" label="JDK" prop="jdkPath">
          <el-input placeholder="JDK home path" v-model="serviceState.configForm.jdkPath"></el-input>
        </el-form-item>
        <el-form-item :label="$t('WORK_HOME_LABEL')" prop="workDirectory">
          <el-input placeholder="work directory" v-model="serviceState.configForm.workDirectory"></el-input>
        </el-form-item>
        <el-form-item :label="$t('ENV_LABEL')" prop="env">
          <el-input
            placeholder="eg: ENV1=val1,ENV2=val2"
            auto-complete="off"
            auto-correct="off"
            auto-capitalize="off"
            v-model="serviceState.configForm.env"></el-input>
        </el-form-item>
        <el-form-item :label="$t('PRIORITY_LABEL')" prop="priority">
          <el-input-number :min="1" :max="9999" v-model="serviceState.configForm.priority"></el-input-number>
        </el-form-item>
        <el-form-item :label="$t('SCHEDULE_TYPE')" prop="daemon">
          <el-radio-group v-model="serviceState.configForm.scheduleType">
            <el-radio label="once">{{ $t('SCHEDULE_ONCE') }}</el-radio>
            <el-radio label="long-times">{{ $t('SCHEDULE_LONE_TIME') }}</el-radio>
            <el-radio label="cron">{{ $t('SCHEDULE_CRON') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-show="'long-times' === serviceState.configForm.scheduleType" :label="$t('DAEMON_LABEL')" prop="daemon">
          <el-switch v-model="serviceState.configForm.daemon"></el-switch>
        </el-form-item>
        <el-form-item v-show="'long-times' === serviceState.configForm.scheduleType" :label="$t('JAR_UPDATE_WATCH_LABEL')" prop="jarUpdateWatch">
          <el-switch v-model="serviceState.configForm.jarUpdateWatch"></el-switch>
        </el-form-item>
        <el-form-item v-show="'cron' === serviceState.configForm.scheduleType" :label="'cron'" prop="cron">
          <cron-input v-model="serviceState.configForm.cron"></cron-input>
        </el-form-item>
        <el-form-item :label="$t('FILE')">
          <el-empty v-if="serviceState.isNew" style="width: 100%" :description="$t('SAVE_CONFIG_AND_ENABLE_FILE')">
            <el-button type="primary" @click="saveAndInit">{{ $t('SAVE') }}</el-button>
          </el-empty>
          <file-manager
            v-else
            :base-dir="userStore.userDir + '/' + serviceState.configForm.name"
            :with-root="true"
            :head-tools="headTools"
            :row-tools="rowTools"></file-manager>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto">
          <el-button size="small" @click="serviceState.showEdit = false">{{ $t('CANCEL') }}</el-button>
          <el-button size="small" type="primary" @click="saveConfig">{{ $t('SAVE') }}</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElForm, ElMessageBox, ElTree, type FormRules } from 'element-plus';
import { STATUS_STARTING } from '@/common/CommonConst';
import { PUB_TOPIC, pubsub } from '@/views/services/ServerPubsubImpl';
import { WsManager } from '@/common/WsManager';
import { FuncCode } from '@/common/EventConst';
import { useBasicStore, useServiceStore, useUserStore } from '@/stores';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import CloudService from '@/services/CloudService';
import type { FileNode, MsgData, ServerSetting, ServiceInstance } from '@/types';
import { PAGE_SERVICE } from '@/common/route-name-constants';
import { useRoute } from 'vue-router';
import ClusterManager from '@/services/ClusterManager';
import InstanceTreeItem from '@/views/services/components/instance-tree-item.vue';

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
  jarUpdateWatch: false,
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
const configRef = ref<InstanceType<typeof ElForm>>();

const route = useRoute();
const basic = useBasicStore();
const serviceStore = useServiceStore();
const userStore = useUserStore();

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
  // 当前选中的节点
  currentNode: [] as ServiceInstance[],
  checked: [] as ServiceInstance[],
  configForm: { ...defaultSetting },
});

const rules = reactive<FormRules>({
  name: [
    { required: true, trigger: 'blur' },
    { min: 1, max: 16, trigger: 'blur' },
  ],
  applicationType: [{ required: true, message: '不可为空', trigger: 'blur' }],
});

const headTools = {
  refresh: true,
  delete: false,
  upload: true,
  addFile: true,
  addFolder: true,
};
const rowTools = {
  download: true,
  edit: true,
  delete: true,
  upload: true,
  addFile: true,
  addFolder: true,
};

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

async function editService(setting: ServerSetting) {
  serviceState.isNew = false;
  serviceState.configForm = setting;
  serviceState.showEdit = true;
}

function newService() {
  serviceState.isNew = true;
  serviceState.configForm = { ...defaultSetting };
  serviceState.showEdit = true;
}

const onCloseEdit = () => {
  serviceState.configForm = { ...defaultSetting };
  serviceState.showVmEdit = false;
};

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

async function saveConfig() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  if (!serviceState.configForm.host) {
    serviceState.configForm.host = (serviceState.activated?.host || basic.host) as string;
  }
  await ClusterManager.saveServerSetting({ ...serviceState.configForm });
  serviceState.showEdit = false;
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

async function saveAndInit() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  if (!serviceState.configForm.host) {
    serviceState.configForm.host = (serviceState.activated?.host || basic.host) as string;
  }
  await ClusterManager.saveServerSetting({ ...serviceState.configForm });
  serviceState.isNew = false;
  // 获取当前选中的节点host
  const inst = { host: serviceState.configForm.host, name: serviceState.configForm.name } as ServiceInstance;
  serviceState.configForm = await ClusterManager.getServerSetting(inst);
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

const onStatusChange = (data: MsgData) => {
  nextTick(() => setStatus(data.sid, data.body));
};
const reload = () => (isService ? serviceStore.reload() : serviceStore.reloadJvmList());

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

function currentChange(data: ServiceInstance, node: any, event: PointerEvent) {
  if (node.isLeaf) {
    const index = serviceState.activatedList.findIndex(item => item.sid === data.sid);
    if (-1 === index) {
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
      console.info('>>>>');
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
  serviceState.activatedList = activatedList;
}

function getSelected() {
  const services = [] as ServiceInstance[];
  return getSelectLoop(serviceState.currentNode, services);
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

function startServices() {
  ClusterManager.startService(getSelected());
}

function stopServices() {
  ClusterManager.stopService(getSelected());
}

function refresh() {
  if (isService) {
    serviceStore.reload();
  } else {
    serviceStore.reloadJvmList();
  }
}

function setStatus(sid: string, status: string) {
  const service = serviceStore.setStatus(sid, status, isService);
  if (service && STATUS_STARTING === status) {
    serviceState.activated = service;
  }
}

function exportServer() {
  if (serviceState.activated.name) {
    if (serviceState.activated.pid) {
      return;
    }
    CommonUtils.exportServer(serviceState.activated.name);
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
    serviceState.importing = true;
    const message = CommonUtils.translate('START_UPLOAD_INFO', { name: file.name });
    CommonNotice.info(message);
    try {
      await CloudService.pushServerDirectory(file);
    } finally {
      serviceState.importing = false;
    }
  };
  input.click();
  input.remove();
}

onMounted(() => {
  reload();
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
  pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
});
onUnmounted(() => {
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, reload);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
  pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
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
  }
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
  .server-content {
    flex: auto;
  }
  .bottom-tab {
    position: absolute;
    bottom: 62px;
    display: flex;
    height: 28px;
    width: 282px;
    background: var(--toolbar-bg-color);
    border: var(--el-border);
    overflow: hidden;
  }
}
</style>
