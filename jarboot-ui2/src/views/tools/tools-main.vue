<template>
  <div class="setting-wrapper">
    <div class="menu-side">
      <el-menu :default-active="state.routeName" class="menu-vertical" :collapse="true" :collapse-transition="true" @select="doSelect">
        <el-menu-item :index="conf.name" v-for="(conf, i) in settingRoutes" :key="i" :route="conf">
          <icon-pro :icon="conf.meta.icon"></icon-pro>
          <template #title>{{ $t(conf.meta.code) }}</template>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="setting-content">
      <router-view v-slot="{ Component }">
        <transition>
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { FILE_MGR, TOOLS } from '@/common/route-name-constants';
import routesConfig from '@/router/routes-config';
import { useUserStore } from '@/stores';

const route = useRoute();
const router = useRouter();
const user = useUserStore();

const state = reactive({
  routeName: '',
});

const settingRoutes = (routesConfig.find(config => TOOLS === config.name)?.children || ([] as any[])).filter(config => {
  if ('jarboot' !== user.username && config?.meta?.code) {
    if (user?.permission && !user.permission[config.meta.code]) {
      return false;
    }
  }
  return true;
});

watch(() => route.name, init);

function init() {
  if (TOOLS === route.name) {
    const name = state.routeName || FILE_MGR;
    router.push({ name });
    state.routeName = name;
    return;
  }
  if ('TOOLS' === route.meta.module) {
    state.routeName = route.name as string;
  }
}

function doSelect(name: string) {
  router.push({ name });
}

onMounted(init);
</script>

<style lang="less" scoped>
@import '@/assets/main.less';
.setting-wrapper {
  display: flex;
  width: 100%;
  .menu-side {
    height: 100%;
  }
  .setting-content {
    flex: auto;
  }
}
.menu-vertical {
  height: calc(100vh - 70px);
}
</style>
