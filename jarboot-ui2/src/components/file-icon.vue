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
      return 'icon-javascript';
    case 'jar':
      return 'icon-jar';
    case 'exe':
      return 'icon-exe';
    case 'py':
      return 'icon-python';
    case 'go':
      return 'icon-golang';
    case 'sh':
    case 'cmd':
    case 'bat':
      return 'icon-terminal';
    case 'tar':
    case 'zip':
    case '7z':
    case 'gz':
      return 'icon-tar';
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
    <icon-pro :icon="getFileIcon()"></icon-pro>
  </span>
</template>
