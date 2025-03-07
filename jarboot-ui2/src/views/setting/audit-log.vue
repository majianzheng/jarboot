<script setup lang="ts">
import { computed } from 'vue';
import type { SearchConfig } from '@/types';
import { useBasicStore } from '@/stores';
import StringUtil from '@/common/StringUtil';
import Request from '@/common/Request';

const searchConfig = computed(
  () =>
    [
      {
        type: 'input',
        name: 'USER_NAME',
        prop: 'username',
      },
      {
        type: 'input',
        name: 'OPERATOR',
        prop: 'operation',
      },
    ] as SearchConfig[]
);

const basic = useBasicStore();
function getList(params: any) {
  return Request.get(`/api/jarboot/auditLog`, params);
}

function formatTime(row: any) {
  return StringUtil.timeFormat(row.createTime);
}
</script>

<template>
  <div>
    <table-pro ref="tableRef" :data-source="getList" :search-config="searchConfig" :height="basic.innerHeight - 190">
      <el-table-column label="用户名" prop="username" width="180"></el-table-column>
      <el-table-column label="操作" prop="operation" show-overflow-tooltip></el-table-column>
      <el-table-column label="方法" prop="method" width="100" show-overflow-tooltip></el-table-column>
      <el-table-column label="参数" prop="argument" show-overflow-tooltip></el-table-column>
      <el-table-column label="IP地址" prop="remoteIp" width="200"></el-table-column>
      <el-table-column label="时间" prop="createTime" width="180" :formatter="formatTime"></el-table-column>
    </table-pro>
  </div>
</template>

<style scoped lang="less"></style>
