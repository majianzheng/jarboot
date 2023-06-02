<template>
  <div>
    <el-form ref="configRef" :role="rules" :model="settingFormData" label-width="auto" status-icon>
      <el-form-item :label="$t('SERVERS_PATH')" prop="workspace">
        <el-input v-model="settingFormData.workspace" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item :label="$t('DEFAULT_VM_OPT')" prop="defaultVmOptions">
        <el-input v-model="settingFormData.defaultVmOptions" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item :label="$t('AUTO_START_AFTER_INIT')" prop="servicesAutoStart">
        <el-input v-model="settingFormData.servicesAutoStart" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button>{{ $t('SUBMIT_BTN') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive } from 'vue';
import SettingService from '@/services/SettingService';

const rules = {};
let settingFormData = reactive({
  workspace: '',
  defaultVmOptions: '',
  servicesAutoStart: false,
});

onMounted(async () => {
  const result: any = await SettingService.getGlobalSetting();
  console.info(result);
  settingFormData = reactive(result);
});
</script>

<style scoped></style>
