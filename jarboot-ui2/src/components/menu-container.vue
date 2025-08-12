<template>
  <div class="setting-wrapper">
    <div class="menu-side" v-if="subMenus?.length">
      <el-menu :default-active="data.key" class="menu-vertical" @select="doSelect" :collapse-transition="true">
        <el-menu-item :class="{ 'is-active': data.key === conf.name }" :index="conf.name" v-for="(conf, i) in subMenus" :key="i">
          <icon-pro :icon="conf.icon"></icon-pro>
          <template #title v-if="!basic.mobileDevice">{{ $t(conf.code) }}</template>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="setting-content">
      <router-view></router-view>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, watch, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useBasicStore } from '@/stores';

const props = defineProps<{
  menu: string;
}>();

const route = useRoute();
const router = useRouter();
const basic = useBasicStore();

const data = reactive({
  key: '',
});

watch(
  () => route.name,
  newValue => (data.key = newValue as string)
);

const subMenus = computed(() => basic.menus.find(config => props.menu === config?.module)?.children || ([] as any[]));

function doSelect(name: string) {
  router.push({ name });
}

onMounted(() => {
  data.key = route.name as string;
});
</script>

<style lang="less" scoped>
@import '@/assets/main.less';
.setting-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  .menu-side {
    height: 100%;
  }
  .setting-content {
    flex: auto;
    padding: 10px;
  }
}
.menu-vertical {
  height: calc(100vh - 60px);
}
</style>
