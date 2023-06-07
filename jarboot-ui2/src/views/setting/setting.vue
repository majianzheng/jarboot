<template>
  <div class="setting-wrapper">
    <div class="menu-side">
      <el-menu :default-active="data.routeName" class="menu-vertical" :collapse="false" :collapse-transition="true" @select="doSelect">
        <el-menu-item :index="conf.name" v-for="(conf, i) in settingRoutes" :key="i">
          <el-icon v-if="PAGE_COMMON === conf.name"><Setting /></el-icon>
          <el-icon v-else><UserFilled /></el-icon>
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
import { PAGE_COMMON, PAGE_SETTING, PAGE_USER } from '@/common/route-name-constants';
import routesConfig from '@/router/routes-config';

const route = useRoute();
const router = useRouter();

const data = reactive({
  routeName: '',
});

const settingRoutes = routesConfig.find(config => PAGE_SETTING === config.name)?.children || ([] as any[]);

watch(() => route.name, init);

function init() {
  if (PAGE_SETTING === route.name) {
    const name = data.routeName || PAGE_COMMON;
    router.push({ name });
    data.routeName = name;
    return;
  }
  if ('SETTING' === route.meta.module) {
    data.routeName = route.name as string;
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
  height: 100%;
  .menu-side {
    width: 120px;
    height: 100%;
  }
  .setting-content {
    flex: auto;
    padding: 10px;
  }
}
.menu-vertical {
  height: calc(100vh - 70px);
}
</style>
