<template>
  <div v-loading="serviceStore.loading" class="__container-wrapper server-mgr">
    <two-sides-pro :show-header="false" :body-height="basic.innerHeight - 50 + 'px'" v-model:collapsed="serviceState.collapsed">
      <template #left-content>
        <div class="server-side">
          <div class="common-bar __tool-bar">
            <div v-if="isService" @click="startServices" class="tool-button tool-button-icon">
              <el-icon class="status-running"><CaretRight /></el-icon>
            </div>
            <div v-if="isService" @click="stopServices" class="tool-button tool-button-icon">
              <el-icon class="tool-button-red-icon"><SwitchButton /></el-icon>
            </div>
            <div @click="serviceStore.reload()" class="tool-button tool-button-icon">
              <el-icon><Refresh /></el-icon>
            </div>
            <div v-if="isService" @click="newService" class="tool-button tool-button-icon">
              <el-icon><Plus /></el-icon>
            </div>
            <div @click="doDashboardCmd" class="tool-button tool-button-red-icon" :class="{ disabled: !serviceState.activated?.sid }">
              <i class="iconfont icon-dashboard"></i>
            </div>
            <div v-if="isService" class="tool-button tool-button-icon" :class="{ disabled: serviceState.importing }" @click="onImport">
              <Loading v-if="serviceState.importing" class="ui-spin" />
              <i v-else class="iconfont icon-import"></i>
            </div>
            <div
              v-if="isService"
              class="tool-button tool-button-icon"
              :class="{ disabled: !serviceState.activated?.sid || serviceState.activated?.pid }"
              @click="exportServer">
              <i class="iconfont icon-export"></i>
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
              @check-change="checkChanged"
              :filter-node-method="filterService">
              <template #default="{ node, data }">
                <div style="width: 100%">
                  <div v-if="node.isLeaf && !data.host" style="width: 100%" @click="currentChange(data, node)">
                    <el-icon v-if="isService && STATUS_STOPPED === data.status" class="status-stopped icon-position"><SuccessFilled /></el-icon>
                    <el-icon v-else-if="STATUS_STARTING === data.status || data.attaching" class="status-starting ui-spin icon-position">
                      <Loading />
                    </el-icon>
                    <el-icon v-else-if="STATUS_STOPPING === data.status" class="status-stopping ui-spin icon-position">
                      <Loading />
                    </el-icon>
                    <el-icon v-else-if="isService && STATUS_STARTED === data.status" class="status-running icon-position">
                      <CaretRight />
                    </el-icon>
                    <el-icon v-else-if="data.attached" class="status-running icon-size">
                      <i class="iconfont icon-debug"></i>
                    </el-icon>
                    <el-icon v-else-if="data.pid" class="status-stopped icon-size">
                      <i class="iconfont icon-debug"></i>
                    </el-icon>
                    <span class="__tree-title" v-if="isService">{{ data.name }}</span>
                    <span class="__tree-title" v-else @dblclick="serviceStore.attach(data.pid)">{{ data.name }}</span>
                    <el-tooltip content="Attach" v-if="!isService && !data.attached">
                      <el-icon @click="serviceStore.attach(data.pid)" class="edit-btn"><Connection /></el-icon>
                    </el-tooltip>
                    <el-tooltip content="Detach" v-if="!isService && data.attached">
                      <el-icon @click="detach(data)" class="edit-btn"><CircleCloseFilled /></el-icon>
                    </el-tooltip>
                    <el-tooltip :content="$t('MODIFY')" v-if="isService">
                      <el-icon class="edit-btn" @click.stop="editService(data)"><Edit /></el-icon>
                    </el-tooltip>
                  </div>
                  <div v-else>
                    <el-icon v-if="data.host" class="group-icon"><HomeFilled /></el-icon>
                    <el-icon v-else class="group-icon"><Folder /></el-icon>
                    <span v-if="data.host" class="__tree-title">{{ data.host }}</span>
                    <span v-else class="__tree-title">{{ data.onlineDebug ? $t(data.name) : data.name || $t('DEFAULT_GROUP') }}</span>
                  </div>
                </div>
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
            :sid="s.sid"
            @close="closeServiceTerminal(s)"
            @execute="cmd => doCommand(s, cmd)"
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
        <el-form-item v-show="'java' === serviceState.configForm.applicationType" :label="$t('VM_OPT_LABEL')" prop="vm">
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
        <el-form-item :label="'运行计划'" prop="daemon">
          <el-radio-group v-model="serviceState.configForm.scheduleType">
            <el-radio label="once">单次执行</el-radio>
            <el-radio label="long-times">长期运行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-show="'long-times' === serviceState.configForm.scheduleType" :label="$t('DAEMON_LABEL')" prop="daemon">
          <el-switch v-model="serviceState.configForm.daemon"></el-switch>
        </el-form-item>
        <el-form-item v-show="'long-times' === serviceState.configForm.scheduleType" :label="$t('JAR_UPDATE_WATCH_LABEL')" prop="jarUpdateWatch">
          <el-switch v-model="serviceState.configForm.jarUpdateWatch"></el-switch>
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElForm, ElMessageBox, ElTree, type FormRules } from 'element-plus';
import { ATTACHED, ATTACHING, EXITED, STATUS_STARTED, STATUS_STARTING, STATUS_STOPPED, STATUS_STOPPING } from '@/common/CommonConst';
import { PUB_TOPIC, pubsub } from '@/views/services/ServerPubsubImpl';
import { WsManager } from '@/common/WsManager';
import { FuncCode } from '@/common/EventConst';
import { useBasicStore, useServiceStore, useUserStore } from '@/stores';
import SettingService from '@/services/SettingService';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import CloudService from '@/services/CloudService';
import type { FileNode, MsgData, ServerSetting, ServiceInstance } from '@/types';
import { PAGE_SERVICE } from '@/common/route-name-constants';
import { useRoute } from 'vue-router';
import Logger from '@/common/Logger';
import { CONSOLE_TOPIC } from '@/types';
import ServiceManager from '@/services/ServiceManager';

const defaultProps = {
  children: 'children',
  label: 'name',
};

const defaultSetting: ServerSetting = {
  name: '',
  group: '',
  applicationType: 'java',
  args: '',
  scheduleType: 'once',
  command: '',
  daemon: false,
  env: '',
  jarUpdateWatch: false,
  jdkPath: '',
  lastModified: 0,
  priority: 0,
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
  return basic.innerWidth - 318;
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

async function editService(row: ServiceInstance) {
  serviceState.isNew = false;
  serviceState.configForm = await SettingService.getServerSetting(row.name);
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

const checkChanged = () => {
  const nodes = treeRef.value?.getCheckedNodes();
  const checked = nodes?.map(node => ({ ...node })) || ([] as any[]);
  checkChange(checked);
};

const doDashboardCmd = () => {
  const service = serviceState.activated;
  service?.sid && doCommand(service, 'dashboard');
};

const doCommand = (service: ServiceInstance, cmd: string) => {
  WsManager.sendMessage({ service: service.name, sid: service.sid, body: cmd, func: FuncCode.CMD_FUNC });
};

const doCancel = (service: ServiceInstance) => {
  WsManager.callFunc(FuncCode.CANCEL_FUNC, service.sid || '');
};

async function saveConfig() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  await SettingService.submitServerSetting({ ...serviceState.configForm });
  serviceState.showEdit = false;
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

async function saveAndInit() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  await SettingService.submitServerSetting({ ...serviceState.configForm });
  serviceState.isNew = false;
  serviceState.configForm = await SettingService.getServerSetting(serviceState.configForm.name);
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

const onStatusChange = (data: MsgData) => {
  setStatus(data.sid, data.body);
};
const reload = () => (isService ? serviceStore.reload() : serviceStore.reloadJvmList());

function detach(server: ServiceInstance) {
  if (!server.sid) {
    return;
  }
  const sid = server.sid || '';
  if (server.remote) {
    ElMessageBox.confirm(CommonUtils.translate('DETACH_MSG'), CommonUtils.translate('WARN'), { type: 'warning' })
      .then(() => {
        WsManager.callFunc(FuncCode.DETACH_FUNC, sid);
      })
      .catch(() => {});
  } else {
    WsManager.callFunc(FuncCode.DETACH_FUNC, sid);
  }
}

function currentChange(data: ServiceInstance, node: any) {
  if (node.isLeaf) {
    const index = serviceState.activatedList.findIndex(item => item.sid === data.sid);
    if (-1 === index) {
      serviceState.activatedList = [...serviceState.activatedList, data];
    }
    serviceState.activated = data;
  }
  serviceState.currentNode = [data];
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
  serviceState.currentNode.forEach((node: ServiceInstance) => {
    if (node?.children && node.children.length > 0) {
      services.push(...node.children);
    } else {
      services.push(node);
    }
  });
  return services;
}

function startServices() {
  ServiceManager.startService(getSelected());
}

function stopServices() {
  ServiceManager.stopService(getSelected());
}

function findInstance(groups: ServiceInstance[], sid: string): ServiceInstance | null {
  for (const g of groups) {
    if (g.sid === sid) {
      return g;
    }
    if (g.children?.length) {
      const s = findInstance(g.children, sid);
      if (s) {
        return s;
      }
    }
  }
  return null;
}

function setStatus(sid: string, status: string) {
  const groups = isService ? serviceStore.groups : serviceStore.jvmGroups;
  const service = findInstance(groups, sid);

  if (service && service.status !== status) {
    if (STATUS_STARTING === status) {
      serviceState.activated = service;
    }
    console.info('status change,', status, service);
    const name = service.name;
    switch (status) {
      case STATUS_STARTING:
        // 激活终端显示
        //activeConsole(key);
        service.status = status;
        Logger.log(`${name} 启动中...`);
        pubsub.publish(sid, CONSOLE_TOPIC.CLEAR_CONSOLE);
        pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
        break;
      case STATUS_STOPPING:
        service.status = status;
        Logger.log(`${name} 停止中...`);
        pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
        break;
      case STATUS_STOPPED:
        service.status = status;
        Logger.log(`${name} 已停止`);
        pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
        break;
      case STATUS_STARTED:
        if (!service.pid) {
          service.status = status;
        }
        Logger.log(`${name} 已启动`);
        pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
        pubsub.publish(sid, PUB_TOPIC.FOCUS_CMD_INPUT);
        break;
      case ATTACHING:
        service.attaching = true;
        service.attached = false;
        Logger.log(`${name} 已停止`);
        pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
        break;
      case ATTACHED:
        service.attached = true;
        service.attaching = false;
        Logger.log(`${name} 已停止`);
        pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
        break;
      case EXITED:
        service.attached = false;
        service.attaching = false;
        Logger.log(`${name} 已停止`);
        pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
        break;
      default:
        return {};
    }
  }
}

function checkChange(checked: ServiceInstance[]) {
  serviceState.checked = checked;
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
    .edit-btn {
      position: absolute;
      font-size: 1.268em;
      right: 10px;
      color: var(--el-color-primary);
    }
  }
  .__tree-title {
    margin-left: 6px;
    position: relative;
    top: -3px;
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
      i.iconfont,
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
  .attached-status {
    color: green;
  }
  .no-attached-status {
    color: gray;
  }
  .icon-position {
    font-size: 1.268em;
    position: relative;
    top: 2px;
  }
  .icon-size {
    font-size: 1.268em;
  }
  .group-icon {
    color: @primary-color;
    font-size: 1.268em;
  }
  .attachedStatus,
  .noAttachedStatus {
    font-size: @tree-icon-size;
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