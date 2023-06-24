<template>
  <div>
    <el-form ref="configRef" :role="rules" :model="state.form" label-width="auto" status-icon>
      <el-form-item :label="$t('DEFAULT_VM_OPT')" prop="defaultVmOptions">
        <el-input v-model="state.form.defaultVmOptions" auto-complete="off" auto-correct="off" auto-capitalize="off"></el-input>
      </el-form-item>
    </el-form>
    <div class="__setting_footer">
      <el-button :loading="state.loading" type="primary" @click="save">{{ $t('SUBMIT_BTN') }}</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive } from 'vue';
import SettingService from '@/services/SettingService';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';

const rules = {};
let state = reactive({
  loading: false,
  form: {
    workspace: '',
    defaultVmOptions: '',
  },
});

async function save() {
  state.loading = true;
  await SettingService.submitGlobalSetting({ ...state.form });
  CommonNotice.success(CommonUtils.translate('SUCCESS'));
  state.loading = false;
}

onMounted(async () => {
  state.form = await SettingService.getGlobalSetting();
});
</script>

<style scoped>
.__setting_footer {
  display: flex;
  justify-content: center;
}
</style>
