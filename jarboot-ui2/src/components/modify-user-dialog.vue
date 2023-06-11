<template>
  <el-dialog :title="$t('RESET_PASSWORD')" v-model="props.visible">
    <el-form :model="modifyUserForm" :rules="rules" label-width="0">
      <el-form-item prop="username">
        <el-input prefix-icon="User" :model-value="username" :placeholder="$t('USER_NAME')" readonly></el-input>
      </el-form-item>
      <el-form-item prop="oldPassword" v-if="'jarboot' !== userStore.username">
        <el-input
          v-model="modifyUserForm.oldPassword"
          prefix-icon="Lock"
          :placeholder="$t('OLD_PASSWORD')"
          @keydown.native.enter="submitForm"
          clearable
          show-password
          type="password"
          autocomplete="off" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="modifyUserForm.password"
          prefix-icon="Lock"
          :placeholder="$t('PASSWORD')"
          @keydown.native.enter="submitForm"
          clearable
          show-password
          type="password"
          autocomplete="off" />
      </el-form-item>
      <el-form-item prop="rePassword">
        <el-input
          v-model="modifyUserForm.rePassword"
          prefix-icon="Lock"
          :placeholder="$t('REPEAT_PASSWORD')"
          @keydown.native.enter="submitForm"
          clearable
          show-password
          type="password"
          autocomplete="off" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="() => emit('update:visible', false)">{{ $t('CANCEL') }}</el-button>
      <el-button type="primary" :loading="loading" @click="submitForm">{{ $t('SUBMIT_BTN') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import { useUserStore } from '@/stores';
import UserService from '@/services/UserService';

const props = defineProps({
  visible: { type: Boolean, default: false },
  username: { type: String, default: '' },
});
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
}>();

const userStore = useUserStore();
const modifyUserForm = reactive({
  username: '',
  oldPassword: '',
  password: '',
  rePassword: '',
});

const validRePassword = (rule: any, value: any, callback: any) => {
  if (value !== modifyUserForm.password) {
    callback(new Error(CommonUtils.translate('PWD_NOT_MATCH')));
  } else {
    callback();
  }
};

const rules = reactive({
  password: [{ required: true, message: CommonUtils.translate('INPUT_USERNAME'), trigger: 'blur' }],
  oldPassword: [{ required: true, message: CommonUtils.translate('OLD_PASSWORD'), trigger: 'blur' }],
  rePassword: [
    { required: true, message: CommonUtils.translate('REPEAT_PASSWORD'), trigger: 'blur' },
    { validator: validRePassword, trigger: 'blur' },
  ],
});

const modifyUserFormRef = ref<FormInstance>();
const loading = ref(false);
const submitForm = async () => {
  if (!modifyUserFormRef.value) return;
  const valid = await modifyUserFormRef.value.validate();
  if (!valid) {
    console.log('error submit!');
    return false;
  }
  loading.value = true;
  try {
    await UserService.updateUserPassword(props.username, modifyUserForm.password, modifyUserForm.oldPassword);
    loading.value = false;
  } finally {
    emit('update:visible', false);
  }
};

onMounted(() => {
  modifyUserForm.username = props.username;
});
</script>

<style scoped></style>
