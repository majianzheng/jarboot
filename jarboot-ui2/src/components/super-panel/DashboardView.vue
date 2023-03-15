<template>
  <el-tabs v-model="state.tab">
    <el-tab-pane label="概览" name="overview">
      <el-row>
        <el-col :span="12">
          <div ref="threadChartRef" :style="{width: '100%', height: (viewHeight / 2) + 'px'}"></div>
        </el-col>
        <el-col :span="12">
          堆内存趋势
        </el-col>
      </el-row>
      <el-row>
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
const MAX_RECORD = 30;

let threadChart = null as unknown as EChartsType;

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

const updateThreadChart = () => {
  const his = [...state.history];
    const NEW = {
      name: 'NEW',
      type: 'line',
      smooth: true,
      data: []
    };
    const RUNNABLE = {
      name: 'RUNNABLE',
      type: 'line',
      stack: 'Total',
      areaStyle: {},
      smooth: true,
      showSymbol: false,
      data: []
    };
    const BLOCKED = {
      name: 'BLOCKED',
      type: 'line',
      stack: 'Total',
      areaStyle: {},
      smooth: true,
      showSymbol: false,
      data: []
    };
    const WAITING = {
      name: 'WAITING',
      type: 'line',
      smooth: true,
      data: []
    };
    const TIMED_WAITING = {
      name: 'TIMED_WAITING',
      type: 'line',
      smooth: true,
      data: []
    };
    const TERMINATED = {
      name: 'TERMINATED',
      type: 'line',
      smooth: true,
      data: []
    };

  const total = {
    name: 'total',
    type: 'line',
    stack: 'Total',
    smooth: true,
    areaStyle: {},
    showSymbol: false,
    data: [] as any[]
  };
  const map = {NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED} as any;
  const states = ['NEW', 'RUNNABLE', 'BLOCKED', 'WAITING', 'TIMED_WAITING', 'TERMINATED'];
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

  const option: EChartsOption = {
    title: {
      text: 'Thread'
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
      data: ['RUNNABLE', 'BLOCKED', 'total']
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
    series: [/*NEW, */RUNNABLE, BLOCKED, /*WAITING, TIMED_WAITING, TERMINATED,*/ total]
  };
  if (his.length <= 1) {
    threadChart.setOption(option);
  } else {
    threadChart.setOption({
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
const updateChart = () => {
  updateThreadChart();
}

onMounted(() => {
  initThreadChart();
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

</style>