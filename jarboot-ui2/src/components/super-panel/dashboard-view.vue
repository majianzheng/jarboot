<template>
  <el-tabs v-model="state.tab">
    <el-tab-pane :label="$t('OVERVIEW')" name="overview">
      <el-row :gutter="5">
        <el-col :span="12">
          <div class="board-panel">
            <div ref="threadChartRef" :style="{ width: '100%', height: chartHeight }"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="board-panel">
            <div ref="heapChartRef" :style="{ width: '100%', height: chartHeight }"></div>
            <span class="mem-board-tool">
              <el-radio-group size="small" v-model="state.memType" @change="updateHeapChart(state.history)">
                <el-radio label="heap" size="small">{{ $t('HEAP') }}</el-radio>
                <el-radio label="nonheap" size="small">{{ $t('NON_HEAP') }}</el-radio>
              </el-radio-group>
              <el-select @change="updateHeapChart(state.history)" size="small" v-if="'heap' === state.memType" v-model="state.heapOption">
                <el-option v-for="opt in state.heapOptions" :value="opt" :key="opt" :label="opt"></el-option>
              </el-select>
              <el-select @change="updateHeapChart(state.history)" size="small" v-else v-model="state.noHeapOption">
                <el-option v-for="opt in state.noHeapOptions" :value="opt" :key="opt" :label="opt"></el-option>
              </el-select>
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="5" style="margin-top: 5px" :style="{ height: viewHeight / 2 + 'px' }">
        <el-col :span="12">
          <div class="board-panel">
            <div ref="cpuChartRef" :style="{ width: '100%', height: chartHeight }"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="board-panel">
            <div class="mem-switch-tool">
              <el-radio-group size="small" v-model="state.memoryType" @change="updateMemChart">
                <el-radio label="heap" size="small">{{ $t('HEAP') }}</el-radio>
                <el-radio label="nonheap" size="small">{{ $t('NON_HEAP') }}</el-radio>
              </el-radio-group>
            </div>
            <div ref="memChartRef" :style="{ width: '100%', height: chartHeight }"></div>
          </div>
        </el-col>
      </el-row>
    </el-tab-pane>
    <el-tab-pane :label="$t('THREAD')" name="threads">
      <el-table :data="props.data.threads" style="width: 100%" :height="viewHeight - 15" size="small" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="NAME" :show-overflow-tooltip="true" />
        <el-table-column prop="group" label="GROUP" width="80" :show-overflow-tooltip="true" />
        <el-table-column prop="priority" label="PRIORITY" width="80" />
        <el-table-column prop="state" label="STATE" width="130">
          <template #default="scope">
            <span :style="{ color: stateColor(scope.row?.state) }">{{ scope.row?.state || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cpu" label="%CPU" width="60">
          <template #default="scope">
            <span :style="{ color: cpuColorFormat(scope.row?.cpu) }">{{ scope.row?.cpu || '0' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deltaTime" label="DELTA_TIME" width="100" />
        <el-table-column prop="time" label="TIME" width="60" />
        <el-table-column prop="interrupted" label="INTERRUPTED" width="120" />
        <el-table-column prop="daemon" label="DAEMON" width="100" />
      </el-table>
    </el-tab-pane>
    <el-tab-pane :label="$t('RUNTIME_INFO')" name="system">
      <el-card :header="$t('RUNTIME_INFO')">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="OS">{{ data.runtimeInfo?.osName }}</el-descriptions-item>
          <el-descriptions-item label="OS Version">{{ data.runtimeInfo?.osVersion }}</el-descriptions-item>
          <el-descriptions-item label="Java Version">{{ data.runtimeInfo?.javaVersion }}</el-descriptions-item>
          <el-descriptions-item label="Java Home">{{ data.runtimeInfo?.javaHome }}</el-descriptions-item>
          <el-descriptions-item label="System LoadAverage">{{ data.runtimeInfo?.systemLoadAverage }}</el-descriptions-item>
          <el-descriptions-item label="Processors">{{ data.runtimeInfo?.processors }}</el-descriptions-item>
          <el-descriptions-item label="Uptime">{{ data.runtimeInfo?.uptime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card header="GC" style="margin-top: 5px">
        <el-table size="small" :data="props.data.gcInfos" style="width: 100%" stripe :height="viewHeight - 350">
          <el-table-column prop="name" label="NAME"></el-table-column>
          <el-table-column prop="collectionCount" label="COUNT"></el-table-column>
          <el-table-column prop="collectionTime" label="TIME"></el-table-column>
        </el-table>
      </el-card>
    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import type { EChartsOption, EChartsType } from 'echarts';
import * as echarts from 'echarts';
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import CommonUtils from '@/common/CommonUtils';
import { useI18n } from 'vue-i18n';
import { debounce, round } from 'lodash';

const { locale } = useI18n();
const props = defineProps<{
  data: any;
  height: number;
  width: number;
}>();
const MB_NUM = 1024 * 1024;
const viewHeight = computed(() => props.height - 5);
const chartHeight = computed(() => (props.height - 30) / 2 + 'px');
const state = reactive({
  tab: 'overview',
  memType: 'heap',
  memoryType: 'heap',
  heapOption: '',
  noHeapOption: '',
  heapOptions: [],
  noHeapOptions: [],
  threadPeakSize: 0,
  cpu: 0,
  history: [] as any[],
});
const threadChartRef = ref<HTMLElement>();
const heapChartRef = ref<HTMLElement>();
const cpuChartRef = ref<HTMLElement>();
const memChartRef = ref<HTMLElement>();

const MAX_RECORD = 500;
const PRECISION = 2;
const COLOR = ['#65B581', '#FFCE34', '#FD665F', '#ea7ccc', '#9a60b4', '#fc8452', '#73c0de', '#ee6666'];

let threadChart = null as unknown as EChartsType;
let heapChart = null as unknown as EChartsType;
let cpuChart = null as unknown as EChartsType;
let memChart = null as unknown as EChartsType;
const resizeChart = () => {
  threadChart?.resize();
  heapChart?.resize();
  cpuChart?.resize();
  memChart?.resize();
};
const darkIconEle = (): HTMLElement => document.getElementById('dark-icon-id') as unknown as HTMLElement;
const checkIsDark = (): boolean => {
  const ele = darkIconEle();
  if (!ele) return false;
  return ele.style.opacity === '1';
};

watch(
  () => props.data,
  (newData: any) => {
    state.heapOptions = (newData.memoryInfo?.heap || []).map((item: any) => item.name);
    state.noHeapOptions = (newData.memoryInfo?.nonheap || []).map((item: any) => item.name);
    if (!state.heapOptions) {
      state.heapOptions = (props.data.memoryInfo?.heap || []).map((item: any) => item.name);
    }
    if (!state.noHeapOptions) {
      state.noHeapOptions = (props.data.memoryInfo?.nonheap || []).map((item: any) => item.name);
    }
    state.history.push({ ...newData });
    if (state.history.length > MAX_RECORD) {
      state.history.shift();
    }
    updateChart();
  }
);

watch(() => [props.height, props.width], debounce(resizeChart, 1000, { maxWait: 3000 }));

const initThreadChart = () => {
  if (threadChartRef.value) {
    threadChart = echarts.init(threadChartRef.value);
  }
};
const initHeapChart = () => {
  if (heapChartRef.value) {
    heapChart = echarts.init(heapChartRef.value);
  }
};
const initCpuChart = () => {
  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value);
  }
};
const initMemChart = () => {
  if (memChartRef.value) {
    memChart = echarts.init(memChartRef.value);
  }
};

const getAfterFixData = (his: any[]): any[] => {
  let last = MAX_RECORD - his.length;
  if (last > (MAX_RECORD * 3) / 4) {
    const n = his.length % 100;
    const expectLength = n > 80 ? 200 + his.length - n : 100 + his.length - n;
    last = expectLength - his.length;
  }
  const afterFixData = [] as any[];
  if (last === 0) {
    return afterFixData;
  }
  const lastItem = his[his.length - 1];
  let timestamp = lastItem.runtimeInfo.timestamp;
  for (let i = 0; i < last; ++i) {
    timestamp += 5000;
    const date = new Date(timestamp);
    afterFixData.push({
      name: date.toString(),
      value: [date, undefined],
    });
  }
  return afterFixData;
};
const creatData = (date: Date, value: number) => {
  return {
    name: date.toString(),
    value: [date, value],
  };
};
const createOption = (title: string, series: any[], subtext: string = '', unit: string = '') => {
  let isDark = checkIsDark();
  (series || []).forEach(item => {
    if (!item.type) {
      item.type = 'line';
    }
    if (undefined === item.smooth) {
      item.smooth = true;
    }
    item.showSymbol = false;
    if (item.lineStyle) {
      item.lineStyle.width = 1;
    } else {
      item.lineStyle = { width: 1 };
    }
  });
  return {
    title: {
      text: title,
      subtext,
      textStyle: {
        color: isDark ? '#eeeeee' : '#313131',
      },
      subtextStyle: {
        color: isDark ? '#d4d4d4' : '#373737',
      },
    },
    color: COLOR,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985',
        },
      },
    },
    legend: {
      data: series.map(i => i.name),
      textStyle: { fontSize: 10, color: isDark ? '#dedede' : '#313131' },
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        color: isDark ? '#dedede' : '#313131',
      },
      splitLine: {
        show: false,
      },
    },
    yAxis: {
      type: 'value',
      boundaryGap: [0, '100%'],
      axisLabel: {
        color: isDark ? '#dedede' : '#313131',
        formatter: '{value}' + unit,
      },
      splitLine: {
        show: false,
      },
    },
    series,
  } as EChartsOption;
};

const updateThreadChart = (his: any[]) => {
  const RUNNABLE = {
    name: CommonUtils.translate('RUNNABLE'),
    data: [],
  };
  const total = {
    name: CommonUtils.translate('TOTAL'),
    data: [] as any[],
  };
  const map = { RUNNABLE } as any;
  const states = ['RUNNABLE'];
  const afterFixData = getAfterFixData(his);
  his.forEach((item: any) => {
    let timestamp = item.runtimeInfo.timestamp;
    let date = new Date(timestamp);
    states.forEach(s => {
      const count = (item.threads || []).filter((thread: any) => s === thread.state).length;
      map[s].data.push(creatData(date, count));
    });
    total.data.push(creatData(date, (item.threads as any[]).length));
  });
  states.forEach(s => {
    if (afterFixData.length > 0) {
      map[s].data.push(...afterFixData);
    }
  });
  if (afterFixData.length > 0) {
    total.data.push(...afterFixData);
  }
  const activeSize = (props.data.threads as any[]).length;
  const peak = Math.max(state.threadPeakSize, activeSize);
  state.threadPeakSize = peak;
  const subtext = `${CommonUtils.translate('ACTIVE')}: ${activeSize}    ${CommonUtils.translate('PEAK_VALUE')}: ${peak}`;
  const option = createOption(CommonUtils.translate('THREAD'), [RUNNABLE, total], subtext);
  threadChart.setOption(option, false, true);
};
const updateHeapChart = (history: any[]) => {
  const submitted = {
    name: CommonUtils.translate('SUBMITTED'),
    data: [] as any[],
  };
  const used = {
    name: CommonUtils.translate('USED'),
    data: [] as any[],
  };
  const afterFixData = getAfterFixData(history);
  const type = 'heap' === state.memType ? state.heapOption : state.noHeapOption;
  history.forEach((item: any) => {
    let timestamp = item.runtimeInfo.timestamp;
    let date = new Date(timestamp);
    const h = ((item?.memoryInfo as any)[state.memType] || []).find((i: any) => type === i.name);
    if (h) {
      submitted.data.push(creatData(date, round(h.total / MB_NUM, PRECISION)));
      used.data.push(creatData(date, round(h.used / MB_NUM, PRECISION)));
    }
  });
  if (afterFixData.length > 0) {
    submitted.data.push(...afterFixData);
    used.data.push(...afterFixData);
  }
  const title = 'heap' === state.memType ? CommonUtils.translate('HEAP_USED') : CommonUtils.translate('NON_HEAP_USED');
  const heap = ((props.data?.memoryInfo as any)[state.memType] || []).find((i: any) => type === i.name);
  const totalSize = round(heap.total / MB_NUM, PRECISION);
  const usedSize = round(heap.used / MB_NUM, PRECISION);
  const maxSize = round(heap.max / MB_NUM, PRECISION);
  const subtext = `${CommonUtils.translate('USED')}: ${usedSize} Mb    ${CommonUtils.translate(
    'SUBMITTED'
  )}: ${totalSize} Mb    ${CommonUtils.translate('MAX')}: ${maxSize} Mb`;
  const option = createOption(title, [submitted, used], subtext, ' Mb');
  heapChart.setOption(option, false, true);
};
const updateCpuChart = (history: any[]) => {
  const submitted = {
    name: 'CPU',
    data: [] as any[],
  };
  const afterFixData = getAfterFixData(history);
  history.forEach((item: any) => {
    let timestamp = item.runtimeInfo.timestamp;
    let date = new Date(timestamp);
    let c = 0;
    (item.threads || []).forEach((thread: any) => {
      c += thread.cpu;
    });
    submitted.data.push(creatData(date, round(c, PRECISION)));
  });
  if (afterFixData.length > 0) {
    submitted.data.push(...afterFixData);
  }
  let cpu = 0;
  (props.data.threads || []).forEach((thread: any) => {
    cpu += thread.cpu;
  });
  state.cpu = cpu;
  const subtext = `${CommonUtils.translate('CPU_USED')}: ${round(cpu, PRECISION)}%`;
  const option = createOption(CommonUtils.translate('CPU_USED'), [submitted], subtext, '%');
  cpuChart.setOption(option, false, true);
};

function updateMemChart() {
  let isDark = checkIsDark();
  const heap = (props.data.memoryInfo[state.memoryType] || []) as any[];
  const total = round(Math.max(heap[0].used / MB_NUM, 0), PRECISION);
  let index = 0;
  const placeHoldData = heap.map((r, i) => {
    const used = round(Math.max(r.used / MB_NUM), PRECISION);
    const value = total - used - index;
    if (i > 0) {
      index = index + used;
    }
    return Math.max(0, value);
  });
  const option = {
    title: {
      text: CommonUtils.translate('MEMORY'),
      subtext: CommonUtils.translate('MEMORY_INFO'),
      textStyle: {
        color: isDark ? '#eeeeee' : '#313131',
      },
      subtextStyle: {
        color: isDark ? '#d4d4d4' : '#373737',
      },
    },

    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985',
        },
      },
      formatter: function (params: any[]) {
        const tar = params[1];
        const max = round(heap[tar.dataIndex].max / MB_NUM, PRECISION);
        const totalValue = round(heap[tar.dataIndex].total / MB_NUM, PRECISION);
        return `${tar.name}<br/>${tar.seriesName} : ${tar.value} Mb<br/>${CommonUtils.translate('SUBMITTED')} : ${Math.max(
          0,
          totalValue
        )} Mb<br/>${CommonUtils.translate('MAX')} : ${Math.max(0, max)} Mb`;
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      axisLabel: { interval: 0, rotate: 30, color: isDark ? '#dedede' : '#313131' },
      splitLine: { show: false },
      data: heap.map(r => r.name),
    },
    yAxis: {
      type: 'value',
      axisLabel: { interval: 0, color: isDark ? '#dedede' : '#313131', formatter: '{value} Mb' },
    },
    series: [
      {
        name: 'Placeholder',
        type: 'bar',
        stack: 'Total',
        itemStyle: {
          borderColor: 'transparent',
          color: 'transparent',
        },
        emphasis: {
          itemStyle: {
            borderColor: 'transparent',
            color: 'transparent',
          },
        },
        data: placeHoldData,
      },
      {
        name: CommonUtils.translate('USED'),
        type: 'bar',
        stack: 'Total',
        label: {
          show: true,
          position: 'inside',
          formatter: function (params) {
            const totalSize = round(heap[params.dataIndex].total / MB_NUM, PRECISION);
            return `${Math.max(0, totalSize)} Mb\n${params.value} Mb`;
          },
        },
        color: COLOR,
        data: heap.map((r, i) => ({ value: round(Math.max(0, r.used / MB_NUM), PRECISION), itemStyle: { color: COLOR[i % COLOR.length] } })),
      },
    ],
  } as EChartsOption;
  memChart.setOption(option, false, true);
}

const observer = new MutationObserver(mutations => {
  mutations.forEach(function (mutation) {
    if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
      nextTick(updateChart);
    }
  });
});

watch(locale, () => nextTick(updateChart));

const updateChart = () => {
  const his = [...state.history];
  updateThreadChart(his);
  updateHeapChart(his);
  updateCpuChart(his);
  updateMemChart();
};

onMounted(() => {
  const element = darkIconEle();
  observer.observe(element, {
    attributes: true, //configure it to listen to attribute changes
  });
  initThreadChart();
  initHeapChart();
  initCpuChart();
  initMemChart();

  state.history.push(props.data);
  state.heapOptions = (props.data.memoryInfo?.heap || []).map((item: any) => item.name);
  state.noHeapOptions = (props.data.memoryInfo?.nonheap || []).map((item: any) => item.name);
  state.heapOption = state.heapOptions[0] || '';
  state.noHeapOption = state.noHeapOptions[0] || '';
  nextTick(updateChart);
});
onUnmounted(() => {
  observer.disconnect();
});
const stateColor = (s: any) => {
  let color;
  switch (s) {
    case 'NEW':
      color = 'cyan';
      break;
    case 'RUNNABLE':
      color = 'green';
      break;
    case 'BLOCKED':
      color = 'red';
      break;
    case 'WAITING':
      color = 'yellow';
      break;
    case 'TIMED_WAITING':
      color = 'magenta';
      break;
    case 'TERMINATED':
      color = 'blue';
      break;
    default:
      break;
  }
  return color;
};
const cpuColorFormat = (cpu: number) => {
  let color;
  if (cpu <= 5) {
    color = 'green';
  } else if (cpu > 5 && cpu <= 15) {
    color = 'magenta';
  } else {
    color = 'red';
  }
  return color;
};
</script>

<style lang="less" scoped>
.board-panel {
  border-radius: 4px;
  opacity: 1;
  background: linear-gradient(180deg, var(--board-panel-body-color) 0%, rgba(229, 240, 252, 0.3) 100%);
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12);
}
.mem-board-tool {
  position: absolute;
  bottom: 0;
  padding-left: 5px;
  .el-select {
    margin: 0 0 2px 5px;
  }
}
.mem-switch-tool {
  position: absolute;
  right: 16px;
  padding-top: 10px;
  z-index: 2;
}
</style>
