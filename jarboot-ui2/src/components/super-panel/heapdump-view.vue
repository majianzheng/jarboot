<template>
  <div>
    <el-result icon="success" title="Heap dump success!" :sub-title="subTitle">
      <template #extra>
        <el-button type="primary" icon="Download" @click="download">{{ $t('DOWNLOAD') }}</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import StringUtil from '@/common/StringUtil';
import { computed } from 'vue';

const props = defineProps<{
  data: any;
  remote: string;
  clusterHost: string | null;
}>();

const subTitle = computed(() => {
  let file = props.data?.dumpFile;
  const isRemote = StringUtil.isNotEmpty(props.remote) && 'localhost' !== props.remote && '127.0.0.1' !== props.remote;
  if (isRemote) {
    file = `Dump file is stored in remote server ${props.remote}, can't download directly.`;
  }
  return file;
});
function download() {
  const path = encodeURIComponent(props.data?.encrypted);
  const url = `/api/jarboot/cluster/manager/download?file=${path}&clusterHost=${props.clusterHost}`;
  const a = document.createElement('a');
  a.href = url;
  a.download = props.data?.dumpFile || 'heapdump.hprof';
  a.click();
  a.remove();
}
</script>
