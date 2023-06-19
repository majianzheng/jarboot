<template>
  <div>
    <table-pro ref="tableRef" :data-source="getList" :search-config="searchConfig" :height="basicStore.innerHeight - 180">
      <template v-slot:right-extra>
        <el-button type="primary" @click="createRole">{{ $t('CREATE') }}</el-button>
      </template>
      <el-table-column :label="$t('ROLE')" prop="role"></el-table-column>
      <el-table-column :label="$t('NAME')" prop="name"></el-table-column>
      <el-table-column :label="$t('OPERATOR')" width="220px">
        <template #default="{ row }">
          <el-button link type="primary" @click="updateRole(row)">{{ $t('MODIFY') }}</el-button>
          <el-button link type="danger" :disabled="ADMIN_ROLE === row.role || SYS_ROLE === row.role" @click="deleteRole(row)">{{
            $t('DELETE')
          }}</el-button>
        </template>
      </el-table-column>
    </table-pro>
    <el-drawer :title="(state.isNew ? $t('CREATE') : $t('MODIFY')) + $t('ROLE')" v-model="state.drawer" destroy-on-close @closed="reset">
      <el-form :model="state.form" label-width="auto" :rules="rules" ref="configRef">
        <el-form-item prop="role" :label="$t('ROLE')">
          <el-input v-model="state.form.role" :disabled="!state.isNew" :placeholder="$t('PLEASE_INPUT') + $t('ROLE')"></el-input>
        </el-form-item>
        <el-form-item prop="name" :label="$t('NAME')">
          <el-input v-model="state.form.name" :placeholder="$t('PLEASE_INPUT') + $t('NAME')"></el-input>
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
import RoleService from '@/services/RoleService';
import { computed, reactive, ref } from 'vue';
import type { SearchConfig } from '@/types';
import { ElForm, ElMessageBox, FormRules } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';
import { useBasicStore } from '@/stores';
import { ADMIN_ROLE, SYS_ROLE } from '@/common/CommonConst';

const searchConfig: SearchConfig[] = [
  {
    type: 'input',
    name: 'ROLE',
    prop: 'role',
  },
  {
    type: 'input',
    name: 'NAME',
    prop: 'name',
  },
];

const resetForm = {
  name: '',
  role: '',
};
const basicStore = useBasicStore();

const state = reactive({
  loading: false,
  isNew: false,
  drawer: false,
  form: { ...resetForm },
});

const rules = computed<FormRules>(() => ({
  name: [
    { required: true, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('NAME'), trigger: 'blur' },
    { min: 1, max: 26, trigger: 'blur' },
  ],
  role: [{ required: true, message: CommonUtils.translate('PLEASE_INPUT') + CommonUtils.translate('ROLE'), trigger: 'blur' }],
}));

const configRef = ref<InstanceType<typeof ElForm>>();
const tableRef = ref();

function reset() {
  state.form = { ...resetForm };
}

function getList(params: any) {
  return RoleService.getRoles(params.role, params.name, params.page, params.limit);
}
function createRole() {
  state.isNew = true;
  state.drawer = true;
}
function updateRole(row: any) {
  state.isNew = false;
  state.form = { ...row };
  state.drawer = true;
}

async function deleteRole(row: any) {
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + row.role + '?', CommonUtils.translate('WARN'), {});
  await RoleService.deleteRole(row.role);
  tableRef.value.refresh();
  CommonNotice.success();
}

async function save() {
  if (!(await configRef.value?.validate())) {
    return;
  }
  state.loading = true;
  try {
    if (state.isNew) {
      await RoleService.addRole(state.form.role, state.form.name);
    } else {
      await RoleService.setRoleName(state.form.role, state.form.name);
    }
    state.drawer = false;
    tableRef.value.refresh();
    CommonNotice.success();
  } finally {
    state.loading = false;
  }
}
</script>
<style scoped></style>
