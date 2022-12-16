<script setup lang="ts">
import { RouterView } from 'vue-router';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import {useI18n} from "vue-i18n";
import {computed} from "vue";

const { locale } = useI18n();
const language = computed(() => (locale.value === 'zh-CN' ? zhCn : en))

</script>

<template>
  <el-config-provider :locale="language">
    <RouterView />
  </el-config-provider>
</template>

<style lang="less" scoped>
</style>
