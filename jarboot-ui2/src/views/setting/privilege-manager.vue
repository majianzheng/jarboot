<script setup lang="ts">
import { useBasicStore } from '@/stores';
import RoleService from '@/services/RoleService';
import { reactive, ref } from 'vue';
import type { Privilege, RoleInfo } from '@/types';
import routesConfig from '@/router/routes-config';
import PrivilegeService from '@/services/PrivilegeService';
import { DEFAULT_PRIVILEGE } from '@/common/CommonConst';

const basicStore = useBasicStore();
const tableRef = ref();
const state = reactive({
  currentRow: {} as RoleInfo,
  data: [] as any[],
});

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
      const permission = (privilegeMap[code] || false) as boolean;
      return {
        authCode: code,
        permission: permission,
        role: state.currentRow.role,
        children: parseTree(conf.children, privilegeMap),
      };
    });
}

async function treeData() {
  if (!state?.currentRow?.role) {
    return [];
  }
  const privilegeList: Privilege[] = (await PrivilegeService.getPrivilegeByRole(state.currentRow.role)) || [];
  const privilegeMap = { ...DEFAULT_PRIVILEGE } as any;
  privilegeList.forEach(privilege => (privilegeMap[privilege.authCode] = privilege.permission));
  const data = parseTree(routesConfig, privilegeMap);
  console.info('>>>>', data, routesConfig, privilegeList, privilegeMap);
  state.data = data;
}
</script>

<template>
  <two-sides-pro :body-height="basicStore.innerHeight - 140 + 'px'" :left-title="$t('ROLE')" :right-title="$t('PRIVILEGE_CONF') + rightTitle()">
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
        :height="basicStore.innerHeight - 210">
        <el-table-column :label="$t('ROLE')" prop="role"></el-table-column>
        <el-table-column :label="$t('NAME')" prop="name"></el-table-column>
      </table-pro>
    </template>
    <template #right-content>
      <el-table v-if="state?.currentRow?.role" :data="state.data" row-key="authCode" default-expand-all :height="basicStore.innerHeight - 210">
        <el-table-column :label="$t('NAME')" prop="authCode" :formatter="(_r, _c, value) => $t(value)"></el-table-column>
        <el-table-column :label="$t('ACCESS_PRIVILEGE')" prop="permission" width="160px">
          <template #default="{ row }">
            <el-switch v-model="row.permission"></el-switch>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else></el-empty>
    </template>
  </two-sides-pro>
</template>

<style scoped lang="less"></style>
