<template>
  <div>
    <el-form :model="modifyUserForm" :rules="rules" label-width="0">
      <el-form-item prop="username">
        <el-input :prefix-icon="User" :placeholder="$t('USER_NAME')" :readonly="StringUtil.isNotEmpty(props.userId)"></el-input>
      </el-form-item>
      <el-form-item prop="oldPassword" v-if="StringUtil.isNotEmpty(props.userId)">
        <el-input
          v-model="modifyUserForm.oldPassword"
          :prefix-icon="Lock"
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
          :prefix-icon="Lock"
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
          :prefix-icon="Lock"
          :placeholder="$t('REPEAT_PASSWORD')"
          @keydown.native.enter="submitForm"
          clearable
          show-password
          type="password"
          autocomplete="off" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import StringUtil from '@/common/StringUtil';
import { User, Lock } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';

const props = defineProps({
  title: { type: String, default: '修改用户密码' },
  visible: { type: Boolean, default: false },
  userId: { type: String, default: '' },
  username: { type: String, default: '' },
});
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
  username: [{ required: true, message: CommonUtils.translate('INPUT_PASSWORD'), trigger: 'blur' }],
  rePassword: [
    { required: true, message: CommonUtils.translate('OLD_PASSWORD'), trigger: 'blur' },
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

  loading.value = false;
};

onMounted(() => {
  modifyUserForm.username = props.username;
});
</script>

<style scoped></style>
