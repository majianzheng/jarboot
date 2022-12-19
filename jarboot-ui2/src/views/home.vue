<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
import CommonConst from "@/common/CommonConst";
import {useUserStore} from "@/stores";
import {onMounted} from "vue";
import {WsManager} from "@/common/WsManager";

const openDoc = () => window.open(CommonConst.DOCS_URL);
const user = useUserStore();
const menus = [
  { path: '/', name: 'SERVICES_MGR'},
  { path: '/diagnose', name: 'ONLINE_DEBUG'},
  { path: '/authority', name: 'AUTH_CONTROL'},
  { path: '/setting', name: 'SETTING'},
];
const welcome = () => {
  console.log(`%c▅▇█▓▒(’ω’)▒▓█▇▅▂`, 'color: magenta');
  console.log(`%c(灬°ω°灬) `, 'color:magenta');
  console.log(`%c（づ￣3￣）づ╭❤～`, 'color:red');
  WsManager.initWebsocket();
};
onMounted(() => {
  welcome();
});

</script>

<template>
  <main>
    <header>
      <img alt="Jarboot logo" class="logo" src="@/assets/logo.png"/>
      <div class="wrapper">
        <nav>
          <RouterLink v-for="menu in menus" :to="menu.path">{{ $t(menu.name) }}</RouterLink>
        </nav>
      </div>
      <div style="flex: auto;"></div>
      <div class="right-extra">
        <div class="tools-box">
          <jarboot-version class="tool-button"></jarboot-version>
          <el-button class="tool-button" size="small" link @click="openDoc">{{ $t('MENU_DOCS') }}</el-button>
          <theme-switch class="tool-button"></theme-switch>
          <language-switch class="tool-button"></language-switch>
        </div>
        <div class="user-avatar">
          <el-dropdown>
            <span>
              <el-avatar>
                <SvgIcon icon="panda" style="width: 26px; height: 26px;"/>
              </el-avatar>
              <el-icon class="el-icon--right"><arrow-down/></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="UserFilled">{{user.username}}</el-dropdown-item>
                <el-dropdown-item icon="Edit">{{$t('MODIFY_PWD')}}</el-dropdown-item>
                <el-dropdown-item icon="Right" @click="user.logout()">{{$t('SIGN_OUT')}}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
    .tools-box {
      margin: 10px 5px;
      border-right: var(--el-border);
    }
    .tool-button {
      margin-right: 15px;
    }
    .user-avatar {
      margin: 2px 10px 0 10px;
      .el-icon--right {
        margin-left: 6px;
        position: relative;
        top: -5px;
      }
    }
  }
}
</style>
