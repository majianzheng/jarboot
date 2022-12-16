<script setup lang="ts">
import {useI18n} from 'vue-i18n';
import { RouterLink, RouterView } from 'vue-router';
import {CommonConst} from "@/common/CommonConst";

const { locale } = useI18n();

const toggle = () => {
  locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN';
  localStorage.setItem('locale', locale.value);
}
const openDoc = () => window.open(CommonConst.DOCS_URL);
const menus = [
  { path: '/', name: 'SERVICES_MGR'},
  { path: '/diagnose', name: 'ONLINE_DEBUG'},
  { path: '/authority', name: 'AUTH_CONTROL'},
  { path: '/setting', name: 'SETTING'},
];
</script>

<template>
  <main>
    <header>
      <img alt="Vue logo" class="logo" src="@/assets/logo.png"/>
      <div class="wrapper">
        <nav>
          <RouterLink v-for="menu in menus" :to="menu.path">{{ $t(menu.name) }}</RouterLink>
        </nav>
      </div>
      <div style="flex: auto;"></div>
      <div class="right-extra">
        <div style="padding: 10px 5px;">
          <jarboot-version class="tool-button"></jarboot-version>
          <el-button class="tool-button" size="small" link @click="openDoc">{{ $t('MENU_DOCS') }}</el-button>
          <theme-switch class="tool-button"></theme-switch>
          <language-switch class="tool-button"></language-switch>
<!--          <el-button class="tool-button" size="small" @click="toggle">{{ $t('navbar.lang') }}</el-button>-->
        </div>
      </div>
    </header>
    <RouterView />
  </main>
</template>
<style lang="less" scoped>
header {
  display: flex;
  height: 50px;
  border-bottom: 1px solid var(--el-border-color);
  .logo {
    height: 38px;
    margin: 6px;
  }
  nav {
    font-size: 16px;
    font-weight: 500;
    text-align: center;
    line-height: 46px;
    a {
      text-decoration: none;
      color: var(--el-text-color-regular);
      padding: 2px 16px;
      display: inline-block;
      &:hover {
        color: var(--el-color-primary-light-3)
      }
      &.router-link-exact-active {
        color: var(--el-color-primary);
        border-bottom: 2px solid var(--el-color-primary);
      }
      &:first-of-type {
        margin-left: 28px;
      }
    }
  }
  .right-extra {
    display: flex;
    .tool-button {
      margin-right: 15px;
    }
  }
}
</style>
