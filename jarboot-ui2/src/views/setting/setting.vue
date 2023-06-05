<template>
  <div class="setting-wrapper">
    <div class="menu-side">
      <el-menu :default-active="data" v-model="data" class="menu-vertical" :collapse="false" :collapse-transition="true" @select="doSelect">
        <el-menu-item :index="PAGE_COMMON">
          <el-icon><setting /></el-icon>
          <template #title>系统配置</template>
        </el-menu-item>
        <el-menu-item :index="PAGE_USER">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
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
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { PAGE_COMMON, PAGE_SETTING, PAGE_USER } from '@/common/route-name-constants';

const route = useRoute();
const router = useRouter();

const data = ref(route.name);
watch(
  () => route.name,
  value => {
    data.value = value;
  }
);

function doSelect(name: string) {
  router.push({ name });
}

onMounted(() => {
  if (PAGE_SETTING === route.name) {
    router.push({ name: PAGE_COMMON });
    data.value = PAGE_COMMON;
    return;
  }
  data.value = route.name;
});
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
