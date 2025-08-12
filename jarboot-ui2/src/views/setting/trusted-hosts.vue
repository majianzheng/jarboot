<script setup lang="ts">
import SettingService from '@/services/SettingService';
import { onMounted, reactive } from 'vue';
import { ElMessageBox } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import CommonNotice from '@/common/CommonNotice';

const state = reactive({
  hosts: [] as any[],
});

async function getList() {
  const hosts = await SettingService.getTrustedHosts();
  state.hosts = (hosts || []).map((host, index) => ({ host, index: index + 1 }));
}
async function add() {
  const ip = await ElMessageBox.prompt('IP', CommonUtils.translate('CREATE'));
  if (ip?.value) {
    await SettingService.addTrustedHost(ip.value);
    CommonNotice.success();
    await getList();
  }
}
async function deleteRow(host: string) {
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + ' ' + host + '?', CommonUtils.translate('WARN'), {});
  await SettingService.removeTrustedHost(host);
  await getList();
  CommonNotice.success();
}
onMounted(getList);
</script>

<template>
  <table-pro :data-source="state.hosts" :total-count="state.hosts.length">
    <template v-slot:right-extra>
      <el-button type="primary" @click="add">{{ $t('CREATE') }}</el-button>
    </template>
    <el-table-column prop="index" label="序号" width="100px"></el-table-column>
    <el-table-column prop="host" label="IP"></el-table-column>
    <el-table-column :label="$t('OPERATOR')">
      <template #default="{ row }">
        <el-button link type="danger" :disabled="'jarboot' === row.username" @click="deleteRow(row.host)">{{ $t('DELETE') }}</el-button>
      </template>
    </el-table-column>
  </table-pro>
</template>
