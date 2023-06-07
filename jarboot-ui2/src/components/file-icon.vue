<script setup lang="ts">
import { canEdit } from '@/components/editor/LangUtils';

const props = defineProps<{
  filename: string;
  directory: boolean;
}>();

function getFileIcon() {
  if (!props.filename || props.directory) {
    return null;
  }
  const i = props.filename?.lastIndexOf('.');
  if (i <= 0) {
    return null;
  }
  const extend = props.filename.substring(i + 1);
  switch (extend) {
    case 'js':
      return 'javascript';
    case 'jar':
      return 'jar';
    case 'exe':
      return 'exe';
    case 'py':
      return 'python';
    case 'go':
      return 'golang';
    case 'sh':
    case 'cmd':
    case 'bat':
      return 'terminal';
    case 'tar':
    case 'zip':
    case '7z':
    case 'gz':
      return 'tar';
  }
  return null;
}

function isImage() {
  if (!props.filename || props.directory) {
    return null;
  }
  return (
    props.filename.endsWith('.png') || props.filename.endsWith('.jpg') || props.filename.endsWith('.bmp') || props.filename.endsWith('.gif')
  );
}
</script>

<template>
  <span>
    <el-icon v-if="directory"><Folder /></el-icon>
    <i v-else-if="getFileIcon()" class="iconfont" :class="'icon-' + getFileIcon()"></i>
    <el-icon v-else-if="isImage()"><PictureFilled /></el-icon>
    <el-icon v-else-if="canEdit(filename)"><Document /></el-icon>
    <i v-else class="iconfont icon-binary"></i>
  </span>
</template>

<style scoped lang="less"></style>
