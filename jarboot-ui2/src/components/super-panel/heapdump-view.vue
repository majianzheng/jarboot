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
import CommonUtils from '@/common/CommonUtils';
import StringUtil from '@/common/StringUtil';

const props = defineProps<{
  data: any;
  remote: string;
}>();

const token = CommonUtils.getRawToken();
let subTitle = props.data?.dumpFile;
const isRemote = StringUtil.isNotEmpty(props.remote) && 'localhost' !== props.remote && '127.0.0.1' !== props.remote;
if (isRemote) {
  subTitle = `Dump file is stored in remote server ${props.remote}, can't download directly.`;
}
function download() {
  const url = `/api/jarboot/cloud/download/${props.data?.encodedName}`;
  CommonUtils.download(url, 'heapdump.hprof');
}
</script>

<style scoped></style>
