<template>
  <div class="setting-wrapper">
    <div class="menu-side">
      <el-menu :default-active="data" v-model="data" class="el-menu-vertical-demo" :collapse="false" :collapse-transition="true" :router="true">
        <el-menu-item index="/setting/common">
          <el-icon><setting /></el-icon>
          <template #title>系统配置</template>
        </el-menu-item>
        <el-menu-item index="/setting/user">
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
import { useRoute } from 'vue-router';

const route = useRoute();

const data = ref(route.path);
watch(
  () => route.path,
  newPath => {
    console.info('path', newPath);
    data.value = newPath;
  }
);
onMounted(() => (data.value = route.path));
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
</style>
