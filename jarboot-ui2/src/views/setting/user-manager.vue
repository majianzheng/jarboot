<template>
  <div>
    <table-pro ref="tableRef" :data-source="getList" :search-config="searchConfig" :height="basicStore.innerHeight - 180">
      <template v-slot:right-extra>
        <el-button type="primary" @click="createUser">{{ $t('CREATE') }}</el-button>
      </template>
      <el-table-column :label="$t('USER_NAME')" prop="username"></el-table-column>
      <el-table-column :label="$t('ROLE')" prop="roles" :formatter="formatRoles" show-overflow-tooltip></el-table-column>
      <el-table-column :label="$t('USER_DIR')" prop="userDir"></el-table-column>
      <el-table-column :label="$t('OPERATOR')">
        <template #default="{ row }">
          <el-button link type="primary" @click="modifyPassword(row)">{{ $t('MODIFY_PWD') }}</el-button>
          <el-button link type="primary" @click="updateUser(row)">{{ $t('MODIFY') }}</el-button>
          <el-button link type="danger" :disabled="'jarboot' === row.username" @click="deleteUser(row)">{{ $t('DELETE') }}</el-button>
        </template>
      </el-table-column>
    </table-pro>
    <el-drawer :title="state.isNew ? $t('CREATE_USER') : $t('MODIFY_USER')" v-model="state.drawer" destroy-on-close @closed="reset">
      <el-form :model="state.form" label-width="auto" :rules="rules" ref="configRef">
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
          <el-input v-model="state.form.username" :disabled="!state.isNew" :placeholder="$t('INPUT_USERNAME')"></el-input>
        </el-form-item>
        <el-form-item prop="fullName" :label="$t('FULL_NAME')">
          <el-input v-model="state.form.fullName" :placeholder="$t('INPUT_USERNAME')"></el-input>
        </el-form-item>
        <el-form-item prop="roles" :label="$t('ROLE')">
          <el-select
            v-model="state.form.roles"
            style="width: 100%"
            :disabled="'jarboot' === state.form.username"
            multiple
            :placeholder="$t('PLEASE_INPUT') + $t('ROLE')">
            <el-option v-for="role in state.roleList" :value="role.role" :label="role.name" :key="role.id">{{ role.name }}</el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="userDir" :label="$t('USER_DIR')">
          <el-autocomplete
            style="width: 100%"
            :fetch-suggestions="queryUserDirs"
            v-model="state.form.userDir"
            :placeholder="$t('PLEASE_INPUT') + $t('USER_DIR')"></el-autocomplete>
        </el-form-item>
        <el-form-item v-if="state.isNew" prop="password" :label="$t('PASSWORD')">
          <el-input v-model="state.form.password" :placeholder="$t('INPUT_PASSWORD')"></el-input>
        </el-form-item>
        <el-form-item v-if="state.isNew" prop="rePassword" :label="$t('RE_PASSWORD')">
          <el-input v-model="state.form.rePassword" :placeholder="$t('REPEAT_PASSWORD')"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="state.drawer = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" :loading="state.loading" @click="save">{{ $t('SAVE') }}</el-button>
      </template>
    </el-drawer>
    <modify-user-dialog v-model:visible="state.dialog" :username="state.modifyUsername" :reset-password="true"></modify-user-dialog>
    <avatar-cutter v-model:visible="state.showCutter" @cancel="state.showCutter = false" return-type="url" @enter="uploadAvatar"></avatar-cutter>
  </div>
</template>

<script lang="ts" setup>
import UserService from '@/services/UserService';
import { computed, onMounted, reactive, ref } from 'vue';
import type { SearchConfig, RoleInfo, SysUser } from '@/types';
import { ElForm, ElMessageBox, type FormRules } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import RoleService from '@/services/RoleService';
import { useBasicStore, useUserStore } from '@/stores';
import { SYS_ROLE } from '@/common/CommonConst';

const searchConfig = computed(
  () =>
    [
      {
        type: 'input',
        name: 'USER_NAME',
        prop: 'username',
      },
      {
        type: 'single-selection',
        name: 'ROLE',
        prop: 'role',
        options: state.roleList.map(role => ({ value: role.role, label: role.name })),
      },
    ] as SearchConfig[]
);

const basicStore = useBasicStore();
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
  isNew: false,
  drawer: false,
  dialog: false,
  modifyUsername: '',
  roleList: [] as RoleInfo[],
  roleMap: {} as any,
  form: { ...resetForm },
  showCutter: false,
});

const validRePassword = (_rule: any, value: any, callback: any) => {
  if (value !== state.form.password) {
    callback(new Error(CommonUtils.translate('PWD_NOT_MATCH')));
  } else {
    callback();
  }
};

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('USER_NAME'), trigger: 'blur' },
    { min: 1, max: 26, trigger: 'blur' },
  ],
  roles: [{ required: true, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('ROLE'), trigger: 'blur' }],
  password: [{ required: state.isNew, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('PASSWORD'), trigger: 'blur' }],
  rePassword: [
    { required: state.isNew, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('RE_PASSWORD'), trigger: 'blur' },
    { validator: validRePassword, trigger: 'blur' },
  ],
}));

const configRef = ref<InstanceType<typeof ElForm>>();
const tableRef = ref();

function reset() {
  state.form = { ...resetForm };
}

function uploadAvatar(avatar: string) {
  state.form.avatar = avatar;
  state.showCutter = false;
}

function formatRoles(_row: any, _col: string, cellValue: string) {
  const roles = cellValue.split(',');
  return roles.map(role => `${state.roleMap[role] || role}`).join(',');
}

function queryUserDirs(query: string, cb: any) {
  UserService.getUserDirs().then((list: string[]) => {
    const filter = query ? list.filter(s => s.includes(query)) : list;
    cb(filter.map(value => ({ value })));
  });
}

function getList(params: any) {
  return UserService.getUsers(params.username, params.role, params.page, params.limit);
}
async function createUser() {
  state.isNew = true;
  await getRoleList();
  state.drawer = true;
}

function modifyPassword(row: SysUser) {
  state.modifyUsername = row.username;
  state.dialog = true;
}
async function updateUser(row: SysUser) {
  state.isNew = false;
  await getRoleList();
  state.form = { ...row, rePassword: '', password: '', roles: row.roles.split(',') };
  state.form.avatar = await UserService.getAvatar(row.username);
  state.drawer = true;
}

async function deleteUser(row: any) {
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + ' ' + row.username + '?', CommonUtils.translate('WARN'), {});
  await UserService.deleteUser(row.id);
  tableRef.value.refresh();
  CommonNotice.success();
}

async function save() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  state.loading = true;
  try {
    const fullName = state.form.fullName || '';
    if (state.isNew) {
      await UserService.createUser(
        state.form.username,
        fullName,
        state.form.password,
        state.form.roles.join(','),
        state.form.userDir,
        state.form.avatar
      );
    } else {
      await UserService.updateUser(state.form.username, fullName, state.form.roles.join(','), state.form.userDir, state.form.avatar);
      if (userStore.username === state.form.username) {
        if (state.form.avatar) {
          userStore.avatar = state.form.avatar;
        }
        if (state.form.fullName) {
          userStore.fullName = state.form.fullName;
        }
      }
    }
    state.drawer = false;
    tableRef.value.refresh();
    // 是否当前用户
    if (userStore.username === state.form.username) {
      userStore.fullName = fullName;
    }
    CommonNotice.success();
  } finally {
    state.loading = false;
  }
}
async function getRoleList() {
  let roleList = await RoleService.getRoleList();
  const roleMap = {} as any;
  roleList.forEach(r => (roleMap[r.role] = r.name));
  state.roleList = roleList.filter(r => SYS_ROLE !== r.role);
  state.roleMap = roleMap;
}
onMounted(getRoleList);
</script>
