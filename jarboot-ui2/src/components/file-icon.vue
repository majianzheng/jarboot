<script setup lang="ts">
import { canEdit } from '@/components/editor/LangUtils';

const props = defineProps<{
  filename: string;
  directory: boolean;
}>();

function getFileIcon(): string {
  if (props.directory) {
    return 'Folder';
  }
  if (!props.filename) {
    return 'icon-binary';
  }
  const i = props.filename?.lastIndexOf('.');
  if (i <= 0) {
    return 'icon-binary';
  }
  const extend = props.filename.substring(i + 1);
  switch (extend) {
    case 'js':
    case 'jsx':
      return 'icon-javascript';
    case 'json':
      return 'icon-json';
    case 'ts':
    case 'tsx':
      return 'icon-typescript';
    case 'jar':
      return 'icon-jar';
    case 'java':
      return 'icon-java';
    case 'css':
    case 'scss':
      return 'icon-css';
    case 'less':
      return 'icon-less';
    case 'py':
      return 'icon-python';
    case 'go':
      return 'icon-golang';
    case 'vue':
      return 'icon-vue';
    case 'exe':
      return 'icon-exe';
    case 'sh':
    case 'cmd':
    case 'bat':
      return 'icon-terminal';
    case 'tar':
    case 'zip':
    case '7z':
    case 'gz':
      return 'icon-tar';
    case 'mp4':
    case 'mv':
    case 'rmvb':
      return 'Film';
    case 'png':
    case 'jpg':
    case 'bmp':
    case 'gif':
      return 'PictureFilled';
  }
  if (canEdit(props.filename)) {
    return 'Document';
  }
  return 'icon-binary';
}
</script>

<template>
  <span>
    <icon-pro :icon="getFileIcon()" :svg="true"></icon-pro>
  </span>
</template>
