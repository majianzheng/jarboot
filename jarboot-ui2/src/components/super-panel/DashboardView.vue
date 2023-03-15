<template>
  <el-tabs v-model="state.tab">
    <el-tab-pane label="概览" name="overview">
      <el-row :gutter="5">
        <el-col :span="12">
          <div class="board-panel" ref="threadChartRef" :style="{width: '100%', height: (viewHeight / 2) + 'px'}"></div>
        </el-col>
        <el-col :span="12">
          <div class="board-panel" ref="heapChartRef" :style="{width: '100%', height: (viewHeight / 2) + 'px'}"></div>
        </el-col>
        <el-col :span="12">
          线程趋势
        </el-col>
        <el-col :span="12">
          堆内存趋势
        </el-col>
      </el-row>
    </el-tab-pane>
    <el-tab-pane label="线程" name="threads">
      <el-table :data="props.data.threads" style="width: 100%" :height="viewHeight" size="small" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="NAME" :show-overflow-tooltip="true"/>
        <el-table-column prop="group" label="GROUP" width="80" :show-overflow-tooltip="true"/>
        <el-table-column prop="priority" label="PRIORITY" width="80" />
        <el-table-column prop="state" label="STATE" width="130">
          <template #default="scope">
            <span :style="{color: stateColor(scope.row?.state)}">{{scope.row?.state || '-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cpu" label="%CPU" width="60">
          <template #default="scope">
            <span :style="{color: cpuColorFormat(scope.row?.cpu)}">{{scope.row?.cpu || '0'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deltaTime" label="DELTA_TIME" width="100" />
        <el-table-column prop="time" label="TIME" width="60" />
        <el-table-column prop="interrupted" label="INTERRUPTED" width="120" />
        <el-table-column prop="daemon" label="DAEMON" width="100" />
      </el-table>
    </el-tab-pane>
    <el-tab-pane label="内存" name="third">

    </el-tab-pane>
    <el-tab-pane label="系统" name="system">

    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import * as echarts from "echarts";
import type {EChartsType, EChartsOption} from "echarts";
import {nextTick, reactive, watch, onMounted, computed, ref} from "vue";
import {useDark} from "@vueuse/core";
import CommonUtils from "@/common/CommonUtils";

const isDark = useDark();
const props = defineProps<{
  data: any;
  height: number;
  width: number;
}>();
const viewHeight = computed(() => props.height - 30);
const state = reactive({
  tab: 'overview',
  history: [] as any[],
});
const threadChartRef = ref();
const heapChartRef = ref();
const MAX_RECORD = 30;

let threadChart = null as unknown as EChartsType;
let heapChart = null as unknown as EChartsType;

const resizeChart = () => {
  threadChart?.resize();
};

watch(() => props.data, (newData: any) => {
  state.history.push({...newData});
  if (state.history.length > MAX_RECORD) {
    state.history.shift();
  }
  updateChart();
});
watch(() => props.height, resizeChart);
watch(() => props.width, resizeChart);

const initThreadChart = () => {
  threadChart = echarts.init(threadChartRef.value, isDark.value ? 'dark' : undefined);
};
const initHeapChart = () => {
  heapChart = echarts.init(heapChartRef.value, isDark.value ? 'dark' : undefined);
};

const updateThreadChart = (his: any[]) => {
    const RUNNABLE = {
      name: CommonUtils.translate('RUNNABLE'),
      type: 'line',
      stack: 'Total',
      areaStyle: {},
      smooth: true,
      showSymbol: false,
      data: []
    };
    const BLOCKED = {
      name: CommonUtils.translate('BLOCKED'),
      type: 'line',
      stack: 'Total',
      areaStyle: {},
      smooth: true,
      showSymbol: false,
      data: []
    };

  const total = {
    name: CommonUtils.translate('TOTAL'),
    type: 'line',
    stack: 'Total',
    smooth: true,
    areaStyle: {},
    showSymbol: false,
    data: [] as any[]
  };
  const map = {RUNNABLE, BLOCKED} as any;
  const states = ['RUNNABLE', 'BLOCKED'];
  const xData = [] as string[];
  his.forEach((item: any) => {
    const timestamp = item.runtimeInfo.timestamp;
    const date = new Date(timestamp);
    states.forEach(state => {
      const count = (item.threads || []).filter((thread: any) => state === thread.state).length;
      map[state].data.push({
        name: date.toString(),
        value: [
          date,
          count
        ]
      });
    });
    total.data.push({
      name: date.toString(),
      value: [
        date,
        (item.threads as any[]).length
      ]
    });
    xData.push(`${date.getHours()}:${date.getMinutes()} ${date.getSeconds()}`);
  });

  const option = {
    title: {
      text: CommonUtils.translate('THREAD')
    },
    darkMode: isDark.value,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['RUNNABLE', 'BLOCKED', 'TOTAL'].map(i => CommonUtils.translate(i))
    },
    xAxis: {
      type: 'time',
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      boundaryGap: [0, '100%'],
      splitLine: {
        show: false
      }
    },
    series: [RUNNABLE, BLOCKED, total]
  } as EChartsOption;
  if (his.length <= 1) {
    threadChart.setOption(option);
  } else {
    threadChart.setOption({
      title: {
        text: CommonUtils.translate('THREAD')
      },
      legend: {
        data: ['RUNNABLE', 'BLOCKED', 'TOTAL'].map(i => CommonUtils.translate(i))
      },
      series: [
        {
          data: RUNNABLE.data
        },
        {
          data: BLOCKED.data
        },
        {
          data: total.data
        }
      ]
    });
  }
};
const updateHeapChart = (history: any[]) => {

};

const updateChart = () => {
  const his = [...state.history];
  updateThreadChart(his);
}

onMounted(() => {
  initThreadChart();
  initHeapChart();
});
const stateColor = (state: any) => {
  let color;
  switch (state) {
    case 'NEW':
      color = "cyan";
      break;
    case 'RUNNABLE':
      color = "green";
      break;
    case 'BLOCKED':
      color = "red";
      break;
    case 'WAITING':
      color = "yellow";
      break;
    case 'TIMED_WAITING':
      color = "magenta";
      break;
    case 'TERMINATED':
      color = "blue";
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
  background: linear-gradient(180deg, #FFFFFF 0%, rgba(229,240,252,0.30) 100%);
  box-shadow: 0px 2px 4px 0px rgba(0, 0, 0, 0.12);
}
</style>