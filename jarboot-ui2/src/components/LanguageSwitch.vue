<template>
<div class="__language-wrapper">
  <el-dropdown>
    <i class="iconfont icon-language"></i>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="row in languageList" @click="toggle(row.value)">{{row.name}}</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</div>
</template>

<script setup lang="ts">

import {computed} from "vue";
import {useI18n} from "vue-i18n";
const { locale } = useI18n();

const toggle = (language: string) => {
  locale.value = language;
  localStorage.setItem('locale', locale.value);
}

const list = [
  {name: '中文', value: 'zh-CN'},
  {name: 'English', value: 'en-US'},
]

const languageList = computed(() => list.filter(row => locale.value != row.value));
</script>

<style lang="less" scoped>
.__language-wrapper {
  display: inline-block;
  .icon-language {
    position: relative;
    top: 2px;
    color: var(--color-hight);
    font-size: var(--el-font-size-extra-large);
  }
}
</style>