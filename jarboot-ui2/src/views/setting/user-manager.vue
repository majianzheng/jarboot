<template>
  <div>
    <table-pro ref="tableRef" :data-source="getList">
      <template v-slot:right-extra>
        <el-button type="primary" @click="createUser">{{ $t('CREATE') }}</el-button>
      </template>
      <el-table-column :label="$t('USER_NAME')" prop="username"></el-table-column>
      <el-table-column :label="$t('OPERATOR')" width="120px">
        <template #default="{ row }">
          <el-button link type="primary">{{ $t('DELETE') }}</el-button>
        </template>
      </el-table-column>
    </table-pro>
  </div>
</template>

<script lang="ts" setup>
import UserService from '@/services/UserService';

function getList(params: any) {
  return UserService.getUsers(params.page, params.limit);
}
function createUser() {
  // todo
}
</script>
<style scoped></style>
