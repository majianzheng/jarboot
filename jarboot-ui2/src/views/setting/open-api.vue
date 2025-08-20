<template>
  <div>
    <table-pro ref="tableRef" :data-source="getList" :search-config="searchConfig" :height="basic.innerHeight - 180">
      <template v-slot:right-extra>
        <el-button type="primary" @click="createToken">{{ $t('CREATE') }}</el-button>
      </template>
      <el-table-column :label="$t('USER_NAME')" prop="username"></el-table-column>
      <el-table-column :label="$t('AUTH_TIME')" prop="expireTime" :formatter="formatExpire"> </el-table-column>
      <el-table-column :label="'TOKEN'" prop="token" show-overflow-tooltip></el-table-column>
      <el-table-column :label="$t('CREATE_TIME')" prop="createTime" width="180px" :formatter="StringUtil.formatRowTime"></el-table-column>
      <el-table-column :label="$t('OPERATOR')" width="120px">
        <template #default="{ row }">
          <el-tooltip placement="top" :content="$t('COPY')">
            <el-button link type="primary" @click="copyToken(row)" icon="CopyDocument"></el-button>
          </el-tooltip>
          <el-tooltip placement="top" :content="$t('DELETE')">
            <el-button link type="danger" icon="Delete" @click="deleteToken(row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </table-pro>
    <el-drawer
      :title="$t('CREATE') + ' Open API Token'"
      v-model="state.drawer"
      :size="basic.mobileDevice ? '100%' : '30%'"
      destroy-on-close
      @closed="reset">
      <el-form :model="state.form" label-width="auto" :rules="rules" ref="configRef">
        <el-form-item :label="$t('USER_NAME')" prop="username">
          <el-input
            v-model="state.form.username"
            prefix-icon="User"
            :placeholder="$t('USER_NAME')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            autocomplete="off" />
        </el-form-item>
        <el-form-item :label="$t('PASSWORD')" prop="password">
          <el-input
            v-model="state.form.password"
            prefix-icon="Lock"
            :placeholder="$t('PASSWORD')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            show-password
            type="password"
            autocomplete="off" />
        </el-form-item>
        <el-form-item :label="$t('AUTH_TIME')" prop="expireTimestamp">
          <el-date-picker
            v-model="state.form.expireTimestamp"
            :disabled-date="disabledDate"
            type="datetime"
            :placeholder="$t('AUTH_TIME')"
            format="YYYY/MM/DD hh:mm:ss"
            value-format="x" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="state.drawer = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" :loading="state.loading" @click="save">{{ $t('SAVE') }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import type { SearchConfig } from '@/types';
import { ElForm, ElMessage, ElMessageBox, type FormRules } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { useBasicStore } from '@/stores';
import UserService from '@/services/UserService';
import StringUtil from '@/common/StringUtil';

const searchConfig: SearchConfig[] = [
  {
    type: 'input',
    name: 'USER_NAME',
    prop: 'username',
  },
];

const resetForm = {
  username: '',
  password: '',
  expireTimestamp: 0,
};
const basic = useBasicStore();

const state = reactive({
  loading: false,
  drawer: false,
  form: { ...resetForm },
  currentTime: Date.now(),
});

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: CommonUtils.translate('INPUT_USERNAME'), trigger: 'blur' }],
  password: [{ required: true, message: CommonUtils.translate('INPUT_PASSWORD'), trigger: 'blur' }],
}));

const configRef = ref<InstanceType<typeof ElForm>>();
const tableRef = ref();

function reset() {
  state.form = { ...resetForm };
}

function getList(params: any) {
  return UserService.getTokens(params.username, params.page, params.limit);
}
function createToken() {
  state.drawer = true;
}

function copyToken(row: any) {
  CommonUtils.copyString(row.token);
  ElMessage({
    message: CommonUtils.translate('COPIED'),
    type: 'success',
  });
}

function formatExpire(row: any): string {
  if (!row.expireTime) {
    return CommonUtils.translate('NO_EXPIRE');
  }
  let str = StringUtil.timeFormat(row.expireTime);
  if (isExpired(row.expireTime)) {
    return CommonUtils.translate('AUTH_EXPIRED', { time: str });
  }
  return CommonUtils.translate('TIME_REMAIN', { time: str, remain: formatExpireTime(row.expireTime) });
}

function isExpired(expireTime: number): boolean {
  return state.currentTime > expireTime;
}
function formatExpireTime(expireTime: number): string {
  let t = expireTime - state.currentTime;
  if (t <= 0) {
    return '';
  }
  t = t / 1000;
  const sec = CommonUtils.translate('SEC');
  const min = CommonUtils.translate('MIN');
  const hour = CommonUtils.translate('HOUR');
  const day = CommonUtils.translate('DAY');
  if (t < 60) {
    return `${t}${sec}`;
  } else if (t < 3600) {
    return `${Math.floor(t / 60)}${min}${Math.floor(t % 60)}${sec}`;
  } else if (t < 86400) {
    return `${Math.floor(t / 3600)}${hour}${Math.floor((t % 3600) / 60)}${min}${Math.floor(t % 60)}${sec}`;
  } else {
    return `${Math.floor(t / 86400)}${day}${Math.floor((t % 86400) / 3600)}${hour}${Math.floor((t % 3600) / 60)}${min}${Math.floor(
      t % 60
    )}${sec}`;
  }
}

function disabledDate(date: Date): boolean {
  const cur = new Date();
  cur.setHours(0);
  cur.setMinutes(0);
  cur.setSeconds(0);
  cur.setMilliseconds(0);
  return date && date.valueOf() < cur.getTime();
}

async function deleteToken(row: any) {
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + '?', CommonUtils.translate('WARN'), {});
  await UserService.removeToken(row.id);
  tableRef.value.refresh();
  CommonNotice.success();
}

async function save() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  state.loading = true;
  try {
    await UserService.createOpenApiToken(state.form.username, state.form.password, state.form.expireTimestamp);
    state.drawer = false;
    tableRef.value.refresh();
    CommonNotice.success();
  } finally {
    state.loading = false;
  }
}
let intervalFd: any;
function updateCurTime() {
  intervalFd = setInterval(() => {
    state.currentTime = Date.now();
  }, 1000);
}

onMounted(() => {
  updateCurTime();
});
onUnmounted(() => {
  clearInterval(intervalFd);
});
</script>
