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
        <div @click="newService" class="tool-button tool-button-icon">
          <el-icon><Plus /></el-icon>
        </div>
        <div class="tool-button tool-button-red-icon"><i class="iconfont icon-dashboard"></i> </div>
        <div class="tool-button tool-button-icon"><i class="iconfont icon-import"></i> </div>
      </div>
      <div style="width: 100%; padding: 3px 1px;">
        <el-input v-model="services.search" placeholder="" prefix-icon="Search" size="small"/>
        <el-tree
            v-show="'service' === services.currentTab"
            ref="treeRef"
            :data="services.groups"
            :props="defaultProps"
            default-expand-all
            highlight-current
            @check-change="checkChanged"
            @node-click="handleNodeClick">
          <template #default="{ node, data }">
            <div style="width: 100%;">
              <div v-if="node.isLeaf" style="width: 100%;" @click="services.currentChange(data, node)">
                <el-icon v-if="CommonConst.STATUS_STOPPED === data.status" class="status-stopped"><SuccessFilled /></el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STARTING === data.status" class="status-starting ui-spin">
                  <Loading/>
                </el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STOPPING === data.status" class="status-stopping ui-spin">
                  <Loading/>
                </el-icon>
                <el-icon v-else-if="CommonConst.STATUS_STARTED === data.status" class="status-running">
                  <CaretRight/>
                </el-icon>
                <span class="__tree-title">{{ data.name }}</span>
                <el-icon class="edit-btn" @click.stop="editService(data)"><Edit /></el-icon>
              </div>
              <div v-else>
                <el-icon class="group-icon">
                  <Menu/>
                </el-icon>
                <span class="__tree-title">{{ data.name || $t('DEFAULT_GROUP') }}</span>
              </div>
            </div>
          </template>
        </el-tree>
      </div>
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
    <el-drawer
        destroy-on-close size="45%"
        :title="(serviceState.isNew ? $t('CREATE'):$t('MODIFY'))"
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
          <el-select v-model="configForm.applicationType">
            <el-option value="java" label="Java"></el-option>
            <el-option value="shell" label="Shell"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('COMMAND_LABEL')" prop="command">
          <el-input
              type="textarea"
              v-model="configForm.command"
              :placeholder="'java'===configForm.applicationType ? $t('COMMAND_EXAMPLE') : 'eg: 1) sh xxx.sh 2) ./app.exe 3) echo Hello!'"></el-input>
        </el-form-item>
        <el-form-item v-show="'java'===configForm.applicationType" :label="$t('VM_OPT_LABEL')" prop="vm">
          <el-input v-model="configForm.vm" spell-check="false">
            <template #append>
              <span>
                <el-button v-if="serviceState.showVmEdit" @click.stop="serviceState.showVmEdit = false">{{$t('CLOSE')}}</el-button>
                <el-button v-else type="text" @click.stop="serviceState.showVmEdit = true">{{$t('MODIFY')}}</el-button>
              </span>
            </template>
          </el-input>
          <div v-if="serviceState.showVmEdit" style="width: 100%">
            <file-editor v-model="configForm.vmContent" height="200px" :name="configForm.vm" mode="text/x-properties"></file-editor>
          </div>
        </el-form-item>
        <el-form-item :label="$t('MAIN_ARGS_LABEL')" prop="args">
          <el-input placeholder="Main arguments" auto-complete="off" auto-correct="off" auto-capitalize="off" v-model="configForm.args"></el-input>
        </el-form-item>
        <el-form-item v-show="'java'===configForm.applicationType" label="JDK" prop="jdkPath">
          <el-input placeholder="JDK home path" v-model="configForm.jdkPath"></el-input>
        </el-form-item>
        <el-form-item :label="$t('WORK_HOME_LABEL')" prop="workDirectory">
          <el-input placeholder="work directory" v-model="configForm.workDirectory"></el-input>
        </el-form-item>
        <el-form-item :label="$t('ENV_LABEL')" prop="env">
          <el-input placeholder="eg: ENV1=val1,ENV2=val2" auto-complete="off" auto-correct="off" auto-capitalize="off" v-model="configForm.env"></el-input>
        </el-form-item>
        <el-form-item :label="$t('DAEMON_LABEL')" prop="daemon">
          <el-switch v-model="configForm.daemon"></el-switch>
        </el-form-item>
        <el-form-item :label="$t('JAR_UPDATE_WATCH_LABEL')" prop="jarUpdateWatch">
          <el-switch v-model="configForm.jarUpdateWatch"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto">
          <el-button size="small" @click="serviceState.showEdit = false">{{$t('CANCEL')}}</el-button>
          <el-button size="small" type="primary" @click="saveConfig">{{$t('SAVE')}}</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import {ref, onMounted, onUnmounted, reactive, h} from 'vue';
import {ElForm, ElTree, FormRules} from 'element-plus';
import CommonConst from '@/common/CommonConst';
import {pubsub, PUB_TOPIC} from "@/views/services/ServerPubsubImpl";
import type {MsgData, ServiceInstance} from "@/common/CommonTypes";
import {WsManager} from "@/common/WsManager";
import {FuncCode} from "@/common/EventConst";
import {useServicesStore} from "@/stores";
import SettingService, {ServerSetting} from "@/services/SettingService";
import CommonUtils from "@/common/CommonUtils";
import CommonNotice from "@/common/CommonNotice";

const defaultProps = {
  children: 'children',
  label: 'name',
};
type ServiceState = {
  isNew: boolean;
  showEdit: boolean;
  showVmEdit: boolean;
};
const treeRef = ref<InstanceType<typeof ElTree>>();
const configRef = ref<InstanceType<typeof ElForm>>();

const services = useServicesStore();
const serviceState = ref<ServiceState>( {
  isNew: false,
  showEdit: false,
  showVmEdit: false,
});
const defaultSetting: ServerSetting = {
  name: '',
  group: '',
  applicationType: 'java',
  args: '',
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
  workspace: ''
};
let configForm = reactive<ServerSetting>(defaultSetting);
const rules = reactive<FormRules>({
  name: [
    {required: true, trigger: 'change'},
    {min: 1, max: 32, trigger: 'change'},
  ],
  applicationType: [{required: true, message: '不可为空', trigger: 'change'},]
});
const editService = async (row: ServiceInstance) => {
  serviceState.value.isNew = false;
  console.info('row:', row)
  const resp = await SettingService.getServerSetting(row.name);
  console.info("resp:", resp);
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
  WsManager.callFunc(FuncCode.CANCEL_FUNC, service.sid || '');
}

const saveConfig = async () => {
  const validate = await configRef.value?.validate();
  if (!validate) {
    return;
  }
  const resp = await SettingService.submitServerSetting({...configForm}).catch(CommonNotice.errorFormatted);
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
    color: var(--el-color-info);
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
