<script setup lang="ts">
import SettingService from '@/services/SettingService';
import { onMounted, reactive } from 'vue';

const state = reactive({
  hosts: [] as any[],
});

async function getList() {
  const hosts = await SettingService.getTrustedHosts();
  state.hosts = (hosts || []).map((host, index) => ({ host, index: index + 1 }));
}
async function add() {
  //
  await SettingService.addTrustedHost('192.168.2.2');
  await getList();
}
onMounted(getList);
</script>

<template>
  <table-pro :data-source="state.hosts" :total-count="state.hosts.length">
    <template v-slot:right-extra>
      <el-button type="primary" @click="add">{{ $t('CREATE') }}</el-button>
    </template>
    <el-table-column prop="index" label="序号" width="100px"></el-table-column>
    <el-table-column prop="host" label="IP地址"></el-table-column>
  </table-pro>
</template>

<style scoped lang="less"></style>
