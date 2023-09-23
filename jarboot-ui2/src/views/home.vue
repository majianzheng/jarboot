<script setup lang="ts">
import { RouterView, useRoute, useRouter } from 'vue-router';
import { DOCS_URL } from '@/common/CommonConst';
import { useUserStore } from '@/stores';
import { onMounted, reactive } from 'vue';
import { WsManager } from '@/common/WsManager';
import routesConfig from '@/router/routes-config';
import StringUtil from '@/common/StringUtil';
import { pubsub } from '@/views/services/ServerPubsubImpl';

const state = reactive({
  dialog: false,
  resetPassword: false,
});

const openDoc = () => window.open(DOCS_URL);
const user = useUserStore();
const route = useRoute();
const router = useRouter();

const filterMenu = (config: any): boolean => {
  if ('jarboot' !== user.username && config?.meta?.code && user?.permission) {
    if (!user.permission[config.meta.code]) {
      return false;
    }
  }
  return config.meta.menu && StringUtil.isNotEmpty(config.meta.module);
};

const menus = routesConfig.filter(config => filterMenu(config)).map(config => ({ name: config.name, module: config.meta.module }));

const welcome = () => {
  console.log(`%c▅▇█▓▒(’ω’)▒▓█▇▅▂`, 'color: magenta');
  console.log(`%c(灬°ω°灬) `, 'color:magenta');
  console.log(`%c（づ￣3￣）づ╭❤～`, 'color:red');
  WsManager.initWebsocket();
  pubsub.init();
};

const isActive = (name: string, module: string): boolean => {
  if (route.name === name) {
    return true;
  }
  return route.meta.module === module;
};

const goTo = (name: string, module: string) => {
  if (route.name !== name && !isActive(name, module)) {
    router.push({ name });
  }
};
function modifyUser() {
  state.resetPassword = false;
  state.dialog = true;
}
function resetPassword() {
  state.resetPassword = true;
  state.dialog = true;
}

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
            :class="{ 'router-link-exact-active': isActive(menu.name, menu.module) }"
            @click="goTo(menu.name, menu.module)"
            >{{ $t(menu.module) }}</a
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
            <div style="display: flex">
              <el-avatar>
                <img v-if="user.avatar" :src="user.avatar" height="40" width="40" alt="avatar" />
                <svg-icon v-else icon="icon-panda" style="width: 26px; height: 26px" />
              </el-avatar>
              <div style="line-height: 40px; margin-left: 5px">
                <span>{{ user.fullName || user.username }}</span>
                <icon-pro icon="ArrowDown" class="el-icon--right"></icon-pro>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="UserFilled" @click="modifyUser">{{ user.fullName || user.username }}</el-dropdown-item>
                <el-dropdown-item icon="Edit" @click="resetPassword">{{ $t('MODIFY_PWD') }}</el-dropdown-item>
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
          <component :is="Component" :key="route.path" v-if="route.meta.keepAlive" />
        </keep-alive>
      </transition>
      <component :is="Component" :key="route.path" v-if="!route.meta.keepAlive" />
    </router-view>
    <modify-user-dialog v-model:visible="state.dialog" :reset-password="state.resetPassword" :username="user.username"></modify-user-dialog>
    <file-upload style="position: fixed; bottom: 60px; right: 15px"></file-upload>
  </main>
</template>
<style lang="less" scoped>
header {
  display: flex;
  height: 50px;
  border-bottom: 1px solid var(--el-border-color);
  .logo {
    height: 38px;
    margin: 6px 15px;
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
      margin: auto 5px;
      border-right: var(--el-border);
    }
    .menu-button {
      margin-right: 15px;
    }
    .user-avatar {
      margin: auto 10px;
      .el-icon--right {
        margin-left: 6px;
        position: relative;
      }
    }
  }
  ._jarboot_username {
    line-height: 40px;
  }
}
</style>
