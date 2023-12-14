<template>
  <div class="__language-wrapper">
    <el-dropdown>
      <el-button size="small" link class="btn-language">
        <icon-pro icon="icon-language" size="20px"></icon-pro>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="(row, i) in languageList" :key="i" @click="toggle(row.value)">{{ row.name }}</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
const { locale } = useI18n();

const toggle = (language: string) => {
  locale.value = language;
  localStorage.setItem('locale', locale.value);
};

const list = [
  { name: '中文', value: 'zh-CN' },
  { name: '繁體', value: 'zh-TW' },
  { name: 'English', value: 'en-US' },
];

const languageList = computed(() => list.filter(row => locale.value != row.value));
</script>

<style lang="less" scoped>
.__language-wrapper {
  display: inline-block;
  .btn-language {
    color: var(--color-hight);
    font-size: var(--el-font-size-extra-large);
  }
}
</style>
