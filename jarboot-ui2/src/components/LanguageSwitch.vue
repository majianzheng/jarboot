<template>
<div class="__language-wrapper">
  <el-popover placement="bottom">
    <template #reference>
      <i class="iconfont icon-language"></i>
    </template>
    <div>
      <div v-for="row in languageList" @click="toggle(row.value)">{{row.name}}</div>
    </div>
  </el-popover>
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
    color: var(--el-text-color-regular);
    font-size: var(--el-font-size-large);
  }
}
</style>