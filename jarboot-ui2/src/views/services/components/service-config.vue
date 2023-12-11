<script setup lang="ts">
import type { FileNode, ServerSetting, ServiceInstance } from '@/types';
import { onMounted, reactive, ref, watch } from 'vue';
import { ElForm, FormRules } from 'element-plus';
import ClusterManager from '@/services/ClusterManager';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';
import { useBasicStore, useUserStore } from '@/stores';

const props = defineProps<{
  setting: ServerSetting;
  activated: ServiceInstance;
  isNew: boolean;
  showEdit: boolean;
  clusterHost?: string;
}>();
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
const rowTools = {
  download: true,
  edit: true,
  delete: true,
  upload: true,
  addFile: true,
  addFolder: true,
};
const rules = reactive<FormRules>({
  name: [
    { required: true, trigger: 'blur' },
    { min: 1, max: 16, trigger: 'blur' },
  ],
  applicationType: [{ required: true, message: '不可为空', trigger: 'blur' }],
});
const userStore = useUserStore();
const basic = useBasicStore();
const state = reactive({
  form: { ...defaultSetting },
  isNew: false,
  showEdit: false,
  showVmEdit: false,
});
const configRef = ref<InstanceType<typeof ElForm>>();

watch(
  () => [props.showEdit, props.isNew, props.setting],
  () => {
    state.showEdit = props.showEdit;
    state.isNew = props.isNew;
    state.form = props.setting;
  }
);
async function saveConfig() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  if (!state.form.host) {
    state.form.host = (props.clusterHost || basic.host) as string;
  }
  await ClusterManager.saveServerSetting({ ...state.form });
  state.showEdit = false;
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

async function saveAndInit() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  if (!state.form.host) {
    state.form.host = (props.clusterHost || basic.host) as string;
  }
  await ClusterManager.saveServerSetting({ ...state.form });
  state.isNew = false;
  // 获取当前选中的节点host
  const inst = { host: state.form.host, name: state.form.name } as ServiceInstance;
  state.form = await ClusterManager.getServerSetting(inst);
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
}

const onCloseEdit = () => {
  state.form = { ...defaultSetting };
  state.showVmEdit = false;
};

onMounted(() => {
  state.form = props.setting;
  state.isNew = props.isNew;
  state.showEdit = props.showEdit;
});
</script>

<template>
  <el-drawer destroy-on-close size="50%" :title="state.isNew ? $t('CREATE') : $t('MODIFY')" @close="onCloseEdit" v-model="state.showEdit">
    <el-form ref="configRef" size="small" :rules="rules" :model="state.form" label-width="auto" label-position="right" status-icon>
      <el-form-item :label="$t('NAME')" prop="name">
        <el-input v-model="state.form.name" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item :label="$t('GROUP')" prop="group">
        <el-input v-model="state.form.group" :placeholder="$t('GROUP_PLACEHOLDER')"></el-input>
      </el-form-item>
      <el-form-item :label="$t('APP_TYPE')" prop="applicationType">
        <el-radio-group v-model="state.form.applicationType">
          <el-radio label="java">Java</el-radio>
          <el-radio label="shell">Shell</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="$t('COMMAND_LABEL')" prop="command">
        <el-input
          type="textarea"
          v-model="state.form.command"
          :placeholder="
            'java' === state.form.applicationType ? $t('COMMAND_EXAMPLE') : 'eg: 1) sh xxx.sh 2) ./app.exe 3) echo Hello!'
          "></el-input>
      </el-form-item>
      <el-form-item v-show="'java' === state.form.applicationType" :label="$t('VM_OPT_LABEL')" prop="vm">
        <el-input v-model="state.form.vm" spell-check="false">
          <template #append>
            <span>
              <el-button v-if="state.showVmEdit" @click.stop="state.showVmEdit = false">{{ $t('CLOSE') }}</el-button>
              <el-button v-else link @click.stop="state.showVmEdit = true">{{ $t('MODIFY') }}</el-button>
            </span>
          </template>
        </el-input>
        <div v-if="state.showVmEdit" style="width: 100%">
          <file-editor v-model="state.form.vmContent" height="200px" :name="state.form.vm" mode="text/x-properties"></file-editor>
        </div>
      </el-form-item>
      <el-form-item :label="$t('MAIN_ARGS_LABEL')" prop="args">
        <el-input placeholder="Main arguments" auto-complete="off" auto-correct="off" auto-capitalize="off" v-model="state.form.args"></el-input>
      </el-form-item>
      <el-form-item v-show="'java' === state.form.applicationType" label="JDK" prop="jdkPath">
        <el-input placeholder="JDK home path" v-model="state.form.jdkPath"></el-input>
      </el-form-item>
      <el-form-item :label="$t('WORK_HOME_LABEL')" prop="workDirectory">
        <el-input placeholder="work directory" v-model="state.form.workDirectory"></el-input>
      </el-form-item>
      <el-form-item :label="$t('ENV_LABEL')" prop="env">
        <el-input
          placeholder="eg: ENV1=val1,ENV2=val2"
          auto-complete="off"
          auto-correct="off"
          auto-capitalize="off"
          v-model="state.form.env"></el-input>
      </el-form-item>
      <el-form-item :label="$t('PRIORITY_LABEL')" prop="priority">
        <el-input-number :min="1" :max="9999" v-model="state.form.priority"></el-input-number>
      </el-form-item>
      <el-form-item :label="$t('SCHEDULE_TYPE')" prop="daemon">
        <el-radio-group v-model="state.form.scheduleType">
          <el-radio label="once">{{ $t('SCHEDULE_ONCE') }}</el-radio>
          <el-radio label="long-times">{{ $t('SCHEDULE_LONE_TIME') }}</el-radio>
          <el-radio label="cron">{{ $t('SCHEDULE_CRON') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-show="'long-times' === state.form.scheduleType" :label="$t('DAEMON_LABEL')" prop="daemon">
        <el-switch v-model="state.form.daemon"></el-switch>
      </el-form-item>
      <el-form-item v-show="'long-times' === state.form.scheduleType" :label="$t('JAR_UPDATE_WATCH_LABEL')" prop="fileUpdateWatch">
        <el-switch v-model="state.form.fileUpdateWatch"></el-switch>
      </el-form-item>
      <el-form-item v-show="'cron' === state.form.scheduleType" :label="'cron'" prop="cron">
        <cron-input v-model="state.form.cron"></cron-input>
      </el-form-item>
      <el-form-item :label="$t('FILE')">
        <el-empty v-if="state.isNew" style="width: 100%" :description="$t('SAVE_CONFIG_AND_ENABLE_FILE')">
          <el-button type="primary" @click="saveAndInit">{{ $t('SAVE') }}</el-button>
        </el-empty>
        <file-manager
          v-else
          :base-dir="userStore.userDir + '/' + state.form.name"
          :with-root="true"
          :cluster-host="state.form.host"
          :row-tools="rowTools"></file-manager>
      </el-form-item>
    </el-form>
    <template #footer>
      <div style="flex: auto">
        <el-button size="small" @click="showEdit = false">{{ $t('CANCEL') }}</el-button>
        <el-button size="small" type="primary" @click="saveConfig">{{ $t('SAVE') }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="less"></style>
