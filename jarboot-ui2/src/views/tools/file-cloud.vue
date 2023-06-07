<script setup lang="ts">
import { useBasicStore } from '@/stores';

const basicStore = useBasicStore();
</script>

<template>
  <div>
    <two-sides-pro :show-header="false" :body-height="basicStore.innerHeight - 72 + 'px'" left-width="380px">
      <template #left-content>
        <file-manager :with-root="true" :base-dir="''"></file-manager>
      </template>
      <template #right-content> 文件内容 </template>
    </two-sides-pro>
  </div>
</template>

<style scoped lang="less"></style>
