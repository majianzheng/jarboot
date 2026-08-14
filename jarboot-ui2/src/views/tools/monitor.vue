<script setup lang="ts">
import Request from '@/common/Request';
import { onMounted, onUnmounted, reactive } from 'vue';
import { floor, round } from 'lodash';
import StringUtil from '@/common/StringUtil';
import { useBasicStore } from '@/stores';
import ClusterManager from '@/services/ClusterManager';

const basic = useBasicStore();

const state = reactive({
  loading: true,
  host: '',
  autoRefresh: true,
  refreshInterval: 5,
  clusterHosts: [] as { host: string; name: string }[],
  server: {} as any,
  CPU: [] as any[],
  mem: [] as any[],
});
const customColors = [
  { color: '#67C23A', percentage: 80 },
  { color: '#E6A23C', percentage: 90 },
  { color: '#F56C6C', percentage: 100 },
];
const idleColors = [
  { color: '#67C23A', percentage: 100 },
  { color: '#E6A23C', percentage: 20 },
  { color: '#F56C6C', percentage: 10 },
];
let timeoutFd: any;
function autoRefresh() {
  if (state.autoRefresh) {
    timeoutFd = setTimeout(async () => {
      await reload();
      timeoutFd = null;
      autoRefresh();
    }, state.refreshInterval * 1000);
  }
}

function setCPU(server: any) {
  const cpuUsed = round((server.cpu.used / server.cpu.total) * 100, 2);
  const sysUsed = round((server.cpu.sys / server.cpu.total) * 100, 2);
  const cpuFree = round((server.cpu.free / server.cpu.total) * 100, 2);
  state.CPU = [
    {
      label: 'CPU_NUM',
      value: server.cpu.cpuNum,
    },
    {
      label: 'CPU_USER',
      value: cpuUsed,
      percentageColor: customColors,
    },
    {
      label: 'CPU_SYS',
      value: sysUsed,
      percentageColor: customColors,
    },
    {
      label: 'CPU_IDLE',
      value: cpuFree,
      percentageColor: idleColors,
    },
  ];
}

function setMEM(server: any) {
  const total = StringUtil.formatBytes(server.mem.total);
  const used = StringUtil.formatBytes(server.mem.used);
  const free = StringUtil.formatBytes(server.mem.free);
  const jvmTotal = StringUtil.formatBytes(server.jvm.total);
  const jvmUsed = StringUtil.formatBytes(server.jvm.total - server.jvm.free);
  const jvmFree = StringUtil.formatBytes(server.jvm.free);
  state.mem = [
    {
      label: 'MEM_TOTAL',
      value: total.fileSize,
      jvm: jvmTotal.fileSize,
    },
    {
      label: 'MEM_USED',
      value: used.fileSize,
      jvm: jvmUsed.fileSize,
    },
    {
      label: 'MEM_FREE',
      value: free.fileSize,
      jvm: jvmFree.fileSize,
    },
    {
      label: 'MEM_USED_PERCENT',
      value: round((server.mem.used / server.mem.total) * 100, 2),
      jvm: round(((server.jvm.total - server.jvm.free) / server.jvm.total) * 100, 2),
      percentageColor: customColors,
    },
  ];
}

async function reload() {
  const params: any = {};
  if (state.host) {
    params['clusterNode'] = state.host;
  }
  const server = await Request.get<any>(`/api/jarboot/monitor/server`, params);
  state.server = server;
  setCPU(server);
  setMEM(server);
}

async function getClusterHosts() {
  state.clusterHosts = await ClusterManager.getOnlineClusterHosts();
  state.host = basic.host;
}

function formatRunTime(time: number, s: string, m: string, h: string, d: string): string {
  let sec = time / 1000;
  if (sec < 60) {
    return `${round(sec, 2)}${s}`;
  }
  let min = sec / 60;
  if (min < 60) {
    sec = sec % 60;
    return `${floor(min)}${m}${floor(sec)}${s}`;
  }
  let hour = min / 60;
  if (hour < 24) {
    sec = sec % 60;
    min = min % 60;
    return `${floor(hour)}${h}${floor(min)}${m}${floor(sec)}${s}`;
  }
  const day = hour / 24;
  hour = hour % 24;
  min = min % 60;
  sec = sec % 60;
  return `${floor(day)}${d}${floor(hour)}${h}${floor(min)}${m}${floor(sec)}${s}`;
}

function init() {
  state.loading = true;
  try {
    reload();
    getClusterHosts();
    autoRefresh();
  } finally {
    state.loading = false;
  }
}

onMounted(init);
onUnmounted(() => {
  state.autoRefresh = false;
  if (timeoutFd) {
    clearTimeout(timeoutFd);
    timeoutFd = null;
  }
});
</script>

<template>
  <div class="monitor" v-loading="state.loading" :style="{ height: `${basic.innerHeight - 120}px` }">
    <div>
      <el-form inline label-suffix=":" size="small">
        <el-form-item :label="$t('CLUSTER_HOST')" v-if="state.clusterHosts?.length">
          <el-select v-model="state.host" style="width: 200px" @change="reload">
            <el-option v-for="item in state.clusterHosts" :key="item.host" :label="item.name" :value="item.host"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('AUTO_REFRESH')">
          <el-switch v-model="state.autoRefresh"></el-switch>
        </el-form-item>
        <el-form-item :label="$t('AUTO_REFRESH_INTERVAL')" v-if="state.autoRefresh">
          <el-input-number v-model="state.refreshInterval" :min="3" :max="60" :step="1" style="width: 130px">
            <template #suffix>{{ $t('SEC') }}</template>
          </el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">{{ $t('REFRESH_BTN') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span><icon-pro icon="icon-cpu"></icon-pro> CPU</span>
          </template>
          <el-table :data="state.CPU">
            <el-table-column :label="$t('PROPERTY')" prop="label">
              <template #default="{ row }">{{ $t(row.label) }}</template>
            </el-table-column>
            <el-table-column :label="$t('VALUE')" prop="value">
              <template #default="{ row }">
                <el-progress v-if="row.percentageColor" :percentage="row.value" :color="row.percentageColor" :stroke-width="15"></el-progress>
                <span v-else>{{ row.value }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span><icon-pro icon="icon-memory"></icon-pro> {{ $t('MEMORY') }}</span>
          </template>
          <el-table :data="state.mem">
            <el-table-column :label="$t('PROPERTY')" prop="label">
              <template #default="{ row }">{{ $t(row.label) }}</template>
            </el-table-column>
            <el-table-column :label="$t('MEMORY')" prop="value">
              <template #default="{ row }">
                <el-progress v-if="row.percentageColor" :percentage="row.value" :color="row.percentageColor" :stroke-width="15"></el-progress>
                <span v-else>{{ row.value }}</span>
              </template>
            </el-table-column>
            <el-table-column label="JVM" prop="jvm">
              <template #default="{ row }">
                <el-progress v-if="row.percentageColor" :percentage="row.jvm" :color="row.percentageColor" :stroke-width="15"></el-progress>
                <span v-else>{{ row.jvm }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card style="margin-top: 20px">
          <template #header>
            <span><icon-pro icon="icon-disk"></icon-pro> {{ $t('DISK_INFO') }}</span>
          </template>
          <div>
            <el-row :gutter="15">
              <el-col :span="12" v-for="(item, i) in state.server.sysFiles" :key="i">
                <div style="padding: 10px 0">
                  <el-descriptions :column="1" border size="small" label-width="180px" :title="`${item.typeName} （${item.dirName}）`">
                    <el-descriptions-item :label="$t('DISK_USAGE')">
                      <el-progress :percentage="item.usage" :color="customColors" :stroke-width="15"></el-progress>
                    </el-descriptions-item>
                    <el-descriptions-item :label="$t('DISK_FORMAT')">{{ item.sysTypeName }}</el-descriptions-item>
                    <el-descriptions-item :label="$t('DISK_TOTAL')">{{ item.total }}</el-descriptions-item>
                    <el-descriptions-item :label="$t('DISK_USED')">{{ item.used }}</el-descriptions-item>
                    <el-descriptions-item :label="$t('DISK_FREE')">{{ item.free }}</el-descriptions-item>
                  </el-descriptions>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card style="margin-top: 20px">
          <template #header>
            <span><icon-pro icon="icon-server"></icon-pro> {{ $t('SERVER_INFO') }}</span>
          </template>
          <el-descriptions v-if="state.server.sys" :column="3" border size="small" label-width="180px">
            <el-descriptions-item :label="$t('SERVER_NAME')">{{ state.server.sys.computerName }}</el-descriptions-item>
            <el-descriptions-item :label="$t('OS_NAME')">{{ state.server.sys.osName }}</el-descriptions-item>
            <el-descriptions-item :label="$t('OS_ARCH')">{{ state.server.sys.osArch }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card style="margin-top: 20px">
          <template #header>
            <span><icon-pro icon="icon-java"></icon-pro> {{ $t('JVM_INFO') }}</span>
          </template>
          <el-descriptions v-if="state.server.jvm" :column="2" border size="small" label-width="180px">
            <el-descriptions-item :label="$t('JVM_RUNTIME_NAME')">{{ state.server.jvm.name }}</el-descriptions-item>
            <el-descriptions-item :label="$t('JVM_VERSION')">{{ state.server.jvm.version }}</el-descriptions-item>
            <el-descriptions-item :label="$t('JVM_START_TIME')">{{ StringUtil.timeFormat(state.server.jvm.startTime) }}</el-descriptions-item>
            <el-descriptions-item :label="$t('JVM_UP_TIME')">
              {{ formatRunTime(state.server.jvm.runTime, $t('SEC'), $t('MIN'), $t('HOUR'), $t('DAY')) }}
            </el-descriptions-item>
            <el-descriptions-item :label="$t('JVM_PATH')">{{ state.server.jvm.home }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="less">
.monitor {
  padding: 15px;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
