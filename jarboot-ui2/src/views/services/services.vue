<template>
  <div v-loading="services.loading" class="__container-wrapper server-mgr">
    <div class="server-side">
      <div class="common-bar __tool-bar">
        <div @click="services.startServices()" class="tool-button tool-button-icon">
          <el-icon class="status-running"><CaretRight /></el-icon>
        </div>
        <div @click="services.stopServices()" class="tool-button tool-button-icon">
          <el-icon class="tool-button-red-icon"><SwitchButton /></el-icon>
        </div>
        <div @click="services.reload()" class="tool-button tool-button-icon">
          <el-icon><Refresh /></el-icon>
        </div>
        <div @click="newService" class="tool-button tool-button-icon">
          <el-icon><Plus /></el-icon>
        </div>
        <div @click="doDashboardCmd" class="tool-button tool-button-red-icon" :class="{ disabled: !services.activated?.sid }">
          <i class="iconfont icon-dashboard"></i>
        </div>
        <div class="tool-button tool-button-icon" :class="{ disabled: serviceState.importing }" @click="onImport">
          <Loading v-if="serviceState.importing" class="ui-spin" />
          <i v-else class="iconfont icon-import"></i>
        </div>
        <div
          class="tool-button tool-button-icon"
          :class="{ disabled: !services.activated?.sid || services.activated?.pid }"
          @click="exportServer">
          <i class="iconfont icon-export"></i>
        </div>
      </div>
      <div style="width: 100%; padding: 3px 1px">
        <el-input v-model="services.search" placeholder="" prefix-icon="Search" size="small" />
        <el-tree
          v-show="'service' === services.currentTab"
          ref="treeRef"
          :data="services.groups"
          :props="defaultProps"
          default-expand-all
          highlight-current
          @check-change="checkChanged">
          <template #default="{ node, data }">
            <div style="width: 100%">
              <div v-if="node.isLeaf" style="width: 100%" @click="services.currentChange(data, node)">
                <el-icon v-if="CommonConst.STATUS_STOPPED === data.status" class="status-stopped"><SuccessFilled /></el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STARTING === data.status || data.attaching" class="status-starting ui-spin">
                  <Loading />
                </el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STOPPING === data.status" class="status-stopping ui-spin">
                  <Loading />
                </el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STARTED === data.status" class="status-running">
                  <CaretRight />
                </el-icon>
                <el-icon v-else-if="data.attached" class="status-running">
                  <i class="iconfont icon-debug"></i>
                </el-icon>
                <el-icon v-else-if="data.pid" class="status-stopped">
                  <i class="iconfont icon-debug"></i>
                </el-icon>
                <span class="__tree-title" @dblclick="services.attach(data.pid)">{{ data.name }}</span>
                <el-tooltip content="Attach" v-if="data.pid && !data.attached">
                  <el-icon @click="services.attach(data.pid)" class="edit-btn"><Connection /></el-icon>
                </el-tooltip>
                <el-tooltip content="Detach" v-if="data.pid && data.attached">
                  <el-icon @click="detach(data)" class="edit-btn"><CircleCloseFilled /></el-icon>
                </el-tooltip>
                <el-tooltip :content="$t('MODIFY')" v-if="!data.pid">
                  <el-icon class="edit-btn" @click.stop="editService(data)"><Edit /></el-icon>
                </el-tooltip>
              </div>
              <div v-else>
                <el-icon v-if="data.host" class="group-icon"><HomeFilled /></el-icon>
                <el-icon v-else class="group-icon"><Platform v-if="data.onlineDebug" /><Folder v-else /></el-icon>
                <span v-if="data.host" class="__tree-title">{{ data.host }}</span>
                <span v-else class="__tree-title">{{ data.onlineDebug ? $t(data.name) : data.name || $t('DEFAULT_GROUP') }}</span>
              </div>
            </div>
          </template>
        </el-tree>
      </div>
    </div>
    <div class="server-content">
      <super-panel
        v-for="(service, i) in services.activatedList"
        :key="i"
        v-show="services.activated.sid === service.sid"
        :sid="service.sid"
        @close="services.closeServiceTerminal(service)"
        @execute="cmd => doCommand(service, cmd)"
        @cancel="doCancel(service)"
        :name="service.name"></super-panel>
      <el-empty v-if="services.activatedList.length === 0" />
    </div>
    <el-drawer
      destroy-on-close
      size="45%"
      :title="serviceState.isNew ? $t('CREATE') : $t('MODIFY')"
      @close="onCloseEdit"
      v-model="serviceState.showEdit">
      <el-form ref="configRef" size="small" :role="rules" :model="configForm" label-width="100px" status-icon>
        <el-form-item :label="$t('NAME')" prop="name">
          <el-input v-model="configForm.name" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
        </el-form-item>
        <el-form-item :label="$t('GROUP')" prop="group">
          <el-input v-model="configForm.group" :placeholder="$t('GROUP_PLACEHOLDER')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('APP_TYPE')" prop="applicationType">
          <el-radio-group v-model="configForm.applicationType">
            <el-radio label="java">Java</el-radio>
            <el-radio label="shell">Shell</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('COMMAND_LABEL')" prop="command">
          <el-input
            type="textarea"
            v-model="configForm.command"
            :placeholder="
              'java' === configForm.applicationType ? $t('COMMAND_EXAMPLE') : 'eg: 1) sh xxx.sh 2) ./app.exe 3) echo Hello!'
            "></el-input>
        </el-form-item>
        <el-form-item v-show="'java' === configForm.applicationType" :label="$t('VM_OPT_LABEL')" prop="vm">
          <el-input v-model="configForm.vm" spell-check="false">
            <template #append>
              <span>
                <el-button v-if="serviceState.showVmEdit" @click.stop="serviceState.showVmEdit = false">{{ $t('CLOSE') }}</el-button>
                <el-button v-else type="text" @click.stop="serviceState.showVmEdit = true">{{ $t('MODIFY') }}</el-button>
              </span>
            </template>
          </el-input>
          <div v-if="serviceState.showVmEdit" style="width: 100%">
            <file-editor v-model="configForm.vmContent" height="200px" :name="configForm.vm" mode="text/x-properties"></file-editor>
          </div>
        </el-form-item>
        <el-form-item :label="$t('MAIN_ARGS_LABEL')" prop="args">
          <el-input
            placeholder="Main arguments"
            auto-complete="off"
            auto-correct="off"
            auto-capitalize="off"
            v-model="configForm.args"></el-input>
        </el-form-item>
        <el-form-item v-show="'java' === configForm.applicationType" label="JDK" prop="jdkPath">
          <el-input placeholder="JDK home path" v-model="configForm.jdkPath"></el-input>
        </el-form-item>
        <el-form-item :label="$t('WORK_HOME_LABEL')" prop="workDirectory">
          <el-input placeholder="work directory" v-model="configForm.workDirectory"></el-input>
        </el-form-item>
        <el-form-item :label="$t('ENV_LABEL')" prop="env">
          <el-input
            placeholder="eg: ENV1=val1,ENV2=val2"
            auto-complete="off"
            auto-correct="off"
            auto-capitalize="off"
            v-model="configForm.env"></el-input>
        </el-form-item>
        <el-form-item :label="'运行计划'" prop="daemon">
          <el-radio-group v-model="configForm.scheduleType">
            <el-radio label="once">单次执行</el-radio>
            <el-radio label="long-times">长期运行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-show="'long-times' === configForm.scheduleType" :label="$t('DAEMON_LABEL')" prop="daemon">
          <el-switch v-model="configForm.daemon"></el-switch>
        </el-form-item>
        <el-form-item v-show="'long-times' === configForm.scheduleType" :label="$t('JAR_UPDATE_WATCH_LABEL')" prop="jarUpdateWatch">
          <el-switch v-model="configForm.jarUpdateWatch"></el-switch>
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
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { ElForm, ElMessageBox, ElTree, type FormRules } from 'element-plus';
import CommonConst from '@/common/CommonConst';
import { pubsub, PUB_TOPIC } from '@/views/services/ServerPubsubImpl';
import type { MsgData, ServiceInstance } from '@/common/CommonTypes';
import { WsManager } from '@/common/WsManager';
import { FuncCode } from '@/common/EventConst';
import { useServicesStore } from '@/stores';
import SettingService, { type ServerSetting } from '@/services/SettingService';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import CloudService from '@/services/CloudService';

const defaultProps = {
  children: 'children',
  label: 'name',
};
type ServiceState = {
  isNew: boolean;
  showEdit: boolean;
  showVmEdit: boolean;
  importing: boolean;
};
const treeRef = ref<InstanceType<typeof ElTree>>();
const configRef = ref<InstanceType<typeof ElForm>>();

const services = useServicesStore();
const serviceState = ref<ServiceState>({
  isNew: false,
  showEdit: false,
  showVmEdit: false,
  importing: false,
});
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
  lastModified: 0n,
  priority: 0,
  sid: '',
  vm: '',
  vmContent: '',
  workDirectory: '',
  workspace: '',
};
let configForm = reactive<ServerSetting>(defaultSetting);
const rules = reactive<FormRules>({
  name: [
    { required: true, trigger: 'change' },
    { min: 1, max: 32, trigger: 'change' },
  ],
  applicationType: [{ required: true, message: '不可为空', trigger: 'change' }],
});
const editService = async (row: ServiceInstance) => {
  serviceState.value.isNew = false;
  const resp = await SettingService.getServerSetting(row.name);
  const data = (resp.result || defaultSetting) as ServerSetting;
  configForm = reactive<ServerSetting>(data);
  serviceState.value.showEdit = true;
};
const newService = () => {
  serviceState.value.isNew = true;
  configForm = reactive<ServerSetting>(defaultSetting);
  serviceState.value.showEdit = true;
};
const onCloseEdit = () => {
  configForm = reactive<ServerSetting>(defaultSetting);
  serviceState.value.showVmEdit = false;
};

const checkChanged = () => {
  const nodes = treeRef.value?.getCheckedNodes();
  const checked = nodes?.map(node => ({ ...node })) || [];
  services.checkChange(checked);
};
const doDashboardCmd = () => {
  const service = services.activated;
  service?.sid && doCommand(service, 'dashboard');
};

const doCommand = (service: ServiceInstance, cmd: string) => {
  WsManager.sendMessage({ service: service.name, sid: service.sid, body: cmd, func: FuncCode.CMD_FUNC });
};
const doCancel = (service: ServiceInstance) => {
  WsManager.callFunc(FuncCode.CANCEL_FUNC, service.sid || '');
};

const saveConfig = async () => {
  const validate = await configRef.value?.validate();
  if (!validate) {
    return;
  }
  const resp = await SettingService.submitServerSetting({ ...configForm }).catch(CommonNotice.errorFormatted);
  serviceState.value.showEdit = false;
  if (0 === resp?.resultCode) {
    CommonNotice.success(CommonUtils.translate('SUCCESS'));
  } else {
    CommonNotice.errorFormatted(resp);
  }
};

const onStatusChange = (data: MsgData) => {
  services.setStatus(data.sid, data.body);
};
const reload = () => services.reload();

const detach = (server: ServiceInstance) => {
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
};

const exportServer = () => {
  if (services.activated.name) {
    if (services.activated.pid) {
      return;
    }
    CommonUtils.exportServer(services.activated.name);
  }
};

const onImport = () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'application/zip';
  input.onchange = () => {
    if (!input.files?.length) {
      return;
    }
    const file = input.files[0];
    serviceState.value.importing = true;
    const message = CommonUtils.translate('START_UPLOAD_INFO', { name: file.name });
    CommonNotice.info(message);
    CloudService.pushServerDirectory(file)
      .then(resp => {
        if (0 !== resp.resultCode) {
          CommonNotice.errorFormatted(resp);
        }
        serviceState.value.importing = false;
      })
      .catch(error => {
        CommonNotice.errorFormatted(error);
        serviceState.value.importing = false;
      });
  };
  input.click();
};

onMounted(() => {
  services.reload();
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
    .edit-btn {
      position: absolute;
      right: 10px;
      color: var(--el-color-primary);
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
