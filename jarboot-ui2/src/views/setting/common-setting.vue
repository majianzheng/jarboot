<template>
  <div class="sys-setting-main">
    <el-form ref="configRef" :role="rules" :model="state.form" label-position="top" status-icon>
      <el-form-item :label="$t('DEFAULT_VM_OPT')" prop="defaultVmOptions">
        <el-input
          v-model="state.form.defaultVmOptions"
          placeholder="Example: -Xms512m -Xmx512m"
          auto-complete="off"
          auto-correct="off"
          auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item label="JDK" prop="jdkPath">
        <el-input
          v-model="state.form.jdkPath"
          placeholder="Default jdk path"
          auto-complete="off"
          auto-correct="off"
          auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item :label="$t('AUTO_START_AFTER_INIT')" prop="servicesAutoStart">
        <el-switch v-model="state.form.servicesAutoStart"></el-switch>
      </el-form-item>
      <el-form-item :label="$t('MAX_START_TIME')" prop="maxStartTime">
        <el-input-number v-model="state.form.maxStartTime" :min="1000"></el-input-number>
      </el-form-item>
      <el-form-item :label="$t('MAX_EXIT_TIME')" prop="maxExitTime">
        <el-input-number v-model="state.form.maxExitTime" :min="1000"></el-input-number>
      </el-form-item>
      <el-form-item :label="$t('AFTER_OFFLINE_EXEC')" prop="afterServerOfflineExec">
        <el-input
          v-model="state.form.afterServerOfflineExec"
          placeholder="sh xxx.sh"
          auto-complete="off"
          auto-correct="off"
          auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item :label="$t('FILE_SHAKE_TIME')" prop="fileChangeShakeTime">
        <el-input-number v-model="state.form.fileChangeShakeTime" :min="3" :max="600"></el-input-number>
      </el-form-item>
    </el-form>
    <div class="__setting_footer">
      <el-button :loading="state.loading" @click="fetchSetting">{{ $t('REFRESH_BTN') }}</el-button>
      <el-button :loading="state.loading" type="primary" @click="save">{{ $t('SUBMIT_BTN') }}</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive } from 'vue';
import SettingService from '@/services/SettingService';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';
import type { GlobalSetting } from '@/types';

const rules = {};
let state = reactive({
  loading: false,
  form: {
    workspace: '',
    defaultVmOptions: '',
    jdkPath: '',
    servicesAutoStart: false,
    maxStartTime: 120000,
    maxExitTime: 30000,
    afterServerOfflineExec: '',
    fileChangeShakeTime: 5,
  } as GlobalSetting,
});

async function save() {
  state.loading = true;
  try {
    await SettingService.submitGlobalSetting({ ...state.form });
    CommonNotice.success(CommonUtils.translate('SUCCESS'));
  } finally {
    state.loading = false;
  }
}

async function fetchSetting() {
  state.form = await SettingService.getGlobalSetting();
}

onMounted(fetchSetting);
</script>

<style lang="less" scoped>
.sys-setting-main {
  padding: 0 15%;
  height: calc(100vh - 76px);
  overflow: auto;
  .__setting_footer {
    display: flex;
    justify-content: center;
  }
}
</style>
