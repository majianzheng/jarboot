<script setup lang="ts">
import { RouterView, useRoute, useRouter } from 'vue-router';
import CommonConst from '@/common/CommonConst';
import { useUserStore } from '@/stores';
import { onMounted } from 'vue';
import { WsManager } from '@/common/WsManager';

const openDoc = () => window.open(CommonConst.DOCS_URL);
const user = useUserStore();
const route = useRoute();
const router = useRouter();

const menus = [
  { path: '/', name: 'SERVICES_MGR', module: '' },
  // { path: '/diagnose', name: 'ONLINE_DEBUG'},
  // { path: '/authority', name: 'AUTH_CONTROL'},
  { path: '/setting/common', name: 'SETTING', module: 'setting' },
];
const welcome = () => {
  console.log(`%c▅▇█▓▒(’ω’)▒▓█▇▅▂`, 'color: magenta');
  console.log(`%c(灬°ω°灬) `, 'color:magenta');
  console.log(`%c（づ￣3￣）づ╭❤～`, 'color:red');
  WsManager.initWebsocket();
};

const isActive = (path: string, module: string): boolean => {
  if (route.path === path) {
    return true;
  }
  return !!module && route.path.includes(module);
};

const goTo = (path: string, isActive: boolean) => {
  if (route.path !== path && !isActive) {
    router.push({ path });
  }
};

onMounted(() => {
  welcome();
});
</script>

<template>
  <main>
    <header>
      <img alt="Jarboot logo" class="logo" src="@/assets/logo.png" />
      <div class="wrapper">
        <nav>
          <a
            v-for="(menu, i) in menus"
            :key="i"
            :class="{ 'router-link-exact-active': isActive(menu.path, menu.module) }"
            @click="goTo(menu.path, isActive(menu.path, menu.module))"
            >{{ $t(menu.name) }}</a
          >
        </nav>
      </div>
      <div style="flex: auto"></div>
      <div class="right-extra">
        <div class="tools-box">
          <div class="menu-button">
            <jarboot-version></jarboot-version>
          </div>
          <div class="menu-button">
            <el-button size="small" link @click="openDoc">{{ $t('MENU_DOCS') }}</el-button>
          </div>
          <div class="menu-button">
            <theme-switch></theme-switch>
          </div>
          <div class="menu-button">
            <language-switch></language-switch>
          </div>
        </div>
        <div class="user-avatar">
          <el-dropdown>
            <span>
              <el-avatar>
                <SvgIcon icon="panda" style="width: 26px; height: 26px" />
              </el-avatar>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="UserFilled">{{ user.username }}</el-dropdown-item>
                <el-dropdown-item icon="Edit">{{ $t('MODIFY_PWD') }}</el-dropdown-item>
                <el-dropdown-item icon="Right" @click="user.logout()">{{ $t('SIGN_OUT') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    <router-view v-slot="{ Component }">
      <transition name="slide-fade">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </transition>
    </router-view>
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
      cursor: pointer;
      &:hover {
        color: var(--el-color-primary-light-3);
      }
      &.router-link-exact-active {
        color: var(--el-color-primary);
        border-bottom: 2px solid var(--el-color-primary);
        cursor: default;
      }
      &:first-of-type {
        margin-left: 28px;
      }
    }
  }
  .right-extra {
    display: flex;
    .tools-box {
      display: flex;
      margin: 10px 5px;
      border-right: var(--el-border);
    }
    .menu-button {
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
