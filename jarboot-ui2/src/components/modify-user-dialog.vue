<template>
  <el-dialog
    :title="resetPassword ? $t('RESET_PASSWORD') : $t('MODIFY_USER')"
    :model-value="props.visible"
    @close="emit('update:visible', false)"
    destroy-on-close>
    <el-form v-if="resetPassword" :model="modifyUserForm" :rules="rules" label-width="0" ref="configRef">
      <el-form-item prop="username">
        <el-input prefix-icon="User" :model-value="fullName || username" :placeholder="$t('USER_NAME')" readonly></el-input>
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
    <el-form v-else :model="state.form" label-width="auto" ref="configRef">
      <el-form-item prop="avatar" label="头像">
        <template #label>
          <span style="line-height: 45px">{{ $t('AVATAR') }}</span>
        </template>
        <el-button plain link @click="state.showCutter = true">
          <el-avatar>
            <img v-if="state.form.avatar" :src="state.form.avatar" height="40" width="40" alt="avatar" />
            <SvgIcon v-else icon="panda" style="width: 26px; height: 26px" />
          </el-avatar>
          <span style="margin-left: 10px">{{ $t('CLICK_MODIFY') }}</span>
        </el-button>
      </el-form-item>
      <el-form-item prop="username" :label="$t('USER_NAME')">
        <el-input v-model="state.form.username" :disabled="true" :placeholder="$t('INPUT_USERNAME')"></el-input>
      </el-form-item>
      <el-form-item prop="fullName" :label="$t('FULL_NAME')">
        <el-input v-model="state.form.fullName" :placeholder="$t('INPUT_FULL_NAME')"></el-input>
      </el-form-item>
      <el-form-item prop="roles" :label="$t('ROLE')">
        <el-select v-model="state.form.roles" style="width: 100%" :disabled="true" multiple :placeholder="$t('PLEASE_INPUT') + $t('ROLE')">
          <el-option v-for="role in state.roleList" :value="role.role" :label="role.name" :key="role.id">{{ role.name }}</el-option>
        </el-select>
      </el-form-item>
      <el-form-item prop="userDir" :label="$t('USER_DIR')">
        <el-input v-model="state.form.userDir" :disabled="true"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="() => emit('update:visible', false)">{{ $t('CANCEL') }}</el-button>
      <el-button type="primary" :loading="loading" @click="submitForm">{{ $t('SUBMIT_BTN') }}</el-button>
    </template>
  </el-dialog>
  <avatar-cutter v-model:visible="state.showCutter" @cancel="state.showCutter = false" return-type="url" @enter="uploadAvatar"></avatar-cutter>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import type { ElForm } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import { useUserStore } from '@/stores';
import UserService from '@/services/UserService';
import type { RoleInfo } from '@/types';
import RoleService from '@/services/RoleService';
import { SYS_ROLE } from '@/common/CommonConst';

const props = defineProps({
  visible: { type: Boolean, default: false },
  username: { type: String, default: '' },
  fullName: { type: String, default: '' },
  resetPassword: { type: Boolean, default: false },
});
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
}>();

const configRef = ref<InstanceType<typeof ElForm>>();
const userStore = useUserStore();
const resetForm = {
  avatar: '',
  username: '',
  fullName: '',
  roles: [] as string[],
  userDir: '',
  password: '',
  rePassword: '',
};
const state = reactive({
  loading: false,
  drawer: false,
  dialog: false,
  modifyUsername: '',
  roleList: [] as RoleInfo[],
  roleMap: {} as any,
  form: { ...resetForm },
  showCutter: false,
});
const modifyUserForm = reactive({
  username: '',
  oldPassword: '',
  password: '',
  rePassword: '',
});

const validRePassword = (_rule: any, value: any, callback: any) => {
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

const loading = ref(false);

function uploadAvatar(avatar: string) {
  state.form.avatar = avatar;
  state.showCutter = false;
}

const submitForm = async () => {
  if (!configRef.value) return;
  const valid = await configRef.value.validate();
  if (!valid) {
    console.log('error submit!');
    return false;
  }
  loading.value = true;
  try {
    if (props.resetPassword) {
      await UserService.updateUserPassword(props.username, modifyUserForm.password, modifyUserForm.oldPassword);
    } else {
      await UserService.updateUser(props.username, state.form.fullName, null, null, state.form.avatar);
      if (state.form.avatar) {
        userStore.avatar = state.form.avatar;
      }
      if (state.form.fullName) {
        userStore.fullName = state.form.fullName;
      }
    }
    loading.value = false;
  } finally {
    emit('update:visible', false);
  }
};

async function getRoleList() {
  let roleList = await RoleService.getRoleList();
  const roleMap = {} as any;
  roleList.map(r => (roleMap[r.role] = r.name));
  state.roleList = roleList.filter(r => SYS_ROLE !== r.role);
  state.roleMap = roleMap;
}
onMounted(async () => {
  modifyUserForm.username = props.username;
  if (!props.resetPassword) {
    await getRoleList();
    state.form.username = userStore.username;
    state.form.avatar = userStore.avatar || '';
    state.form.fullName = userStore.fullName;
    state.form.roles = userStore.roles.split(',');
    state.form.userDir = userStore.userDir;
  }
});
</script>

<style scoped></style>
