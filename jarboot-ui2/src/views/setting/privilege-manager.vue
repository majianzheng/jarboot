<script setup lang="ts">
import { useBasicStore, useUserStore } from '@/stores';
import RoleService from '@/services/RoleService';
import { onMounted, reactive, ref, watch } from 'vue';
import type { Privilege, RoleInfo } from '@/types';
import routesConfig from '@/router/routes-config';
import PrivilegeService from '@/services/PrivilegeService';
import { DEFAULT_PRIVILEGE, SYS_ROLE } from '@/common/CommonConst';
import { debounce } from 'lodash';
import { useI18n } from 'vue-i18n';

const { locale } = useI18n();
const basicStore = useBasicStore();
const userStore = useUserStore();
const tableRef = ref();

const state = reactive({
  currentRow: {} as RoleInfo,
  data: [] as any[],
  totalWidth: basicStore.innerWidth - 180,
});
watch(() => [basicStore.innerHeight, basicStore.innerWidth, locale.value], debounce(resize, 500, { maxWait: 1000 }));

function getList(params: any) {
  return RoleService.getRoles(params.role, params.name, params.page, params.limit);
}
async function currentChange(row: RoleInfo) {
  state.currentRow = row;
  await treeData();
}
function reload() {
  tableRef.value?.refresh();
}

function rightTitle() {
  if (!state?.currentRow?.role) {
    return '';
  }
  return ` (${state?.currentRow?.name || state.currentRow.role})`;
}

function parseTree(config: any[], privilegeMap: any): any[] {
  if (!config?.length) {
    return [];
  }
  return config
    .filter(conf => conf.meta.code)
    .map(conf => {
      const code = conf.meta.code || '';
      const role = state.currentRow.role;
      const permission = SYS_ROLE === role || ((privilegeMap[code] || false) as boolean);
      return {
        authCode: code,
        permission: permission,
        role,
        children: parseTree(conf.children, privilegeMap),
      };
    });
}
function resize() {
  const contentEle = document.querySelector('div.menu-side');
  if (contentEle) {
    state.totalWidth = basicStore.innerWidth - contentEle.clientWidth - 30;
  } else {
    state.totalWidth = basicStore.innerWidth - 180;
  }
}
async function treeData() {
  if (!state?.currentRow?.role) {
    return [];
  }
  const privilegeList: Privilege[] = (await PrivilegeService.getPrivilegeByRole(state.currentRow.role)) || [];
  const privilegeMap = { ...DEFAULT_PRIVILEGE } as any;
  privilegeList.forEach(privilege => (privilegeMap[privilege.authCode] = privilege.permission));
  state.data = parseTree(routesConfig, privilegeMap);
}
async function changePermission(row: any, value: boolean) {
  if (SYS_ROLE === row.role) {
    row.permission = true;
    return;
  }
  try {
    await PrivilegeService.savePrivilege(row.role, row.authCode, value);
    // 权限修改成功
    if (userStore.roles.includes(row.role)) {
      userStore.privileges = { ...DEFAULT_PRIVILEGE } as any;
      await userStore.fetchPrivilege();
    }
  } catch (error) {
    row.permission = !value;
  }
}
onMounted(resize);
</script>

<template>
  <two-sides-pro
    :body-height="basicStore.innerHeight - 120"
    :total-width="state.totalWidth"
    :left-title="$t('ROLE')"
    :right-title="$t('PRIVILEGE_CONF') + rightTitle()">
    <template #left-tools>
      <el-button link type="primary" icon="Refresh" @click="reload">{{ $t('REFRESH_BTN') }}</el-button>
    </template>
    <template #left-content>
      <table-pro
        ref="tableRef"
        :data-source="getList"
        highlight-current-row
        row-key="role"
        @current-change="currentChange"
        pageLayout="prev, pager, next"
        :height="basicStore.innerHeight - 190">
        <el-table-column :label="$t('ROLE')" prop="role"></el-table-column>
        <el-table-column :label="$t('NAME')" prop="name"></el-table-column>
      </table-pro>
    </template>
    <template #right-content>
      <el-table v-if="state?.currentRow?.role" :data="state.data" row-key="authCode" default-expand-all :height="basicStore.innerHeight - 190">
        <el-table-column :label="$t('NAME')" prop="authCode" :formatter="(_r, _c, value) => $t(value)"></el-table-column>
        <el-table-column :label="$t('ACCESS_PRIVILEGE')" prop="permission" width="160px">
          <template #default="{ row }">
            <el-switch
              :disabled="SYS_ROLE === row.role"
              v-model="row.permission"
              @change="value => changePermission(row, value as boolean)"></el-switch>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else></el-empty>
    </template>
  </two-sides-pro>
</template>
