<template>
  <div>
    <div class="table-header">
      <slot name="before-search"></slot>
      <el-form inline v-if="props.searchConfig">
        <slot name="search-form-item-before"></slot>
        <el-form-item v-for="(config, index) in getSearchConfig()" :key="index">
          <el-input
            :key="'input-' + index"
            v-if="config.type === 'input'"
            v-model="state.searchParams[config.prop]"
            clearable
            class="fix-search-width"
            autocomplete="off"
            autocapitalize="off"
            :placeholder="parsePlaceHolder(config)"></el-input>
          <el-input-number
            :key="'input-number-' + index"
            v-else-if="config.type === 'input-number'"
            v-model="state.searchParams[config.prop]"
            clearable
            class="fix-search-width"
            :placeholder="parsePlaceHolder(config)"></el-input-number>
          <el-select
            :key="'select-' + index"
            v-else-if="config.type === 'single-selection'"
            v-model="state.searchParams[config.prop]"
            clearable
            class="fix-search-width"
            :placeholder="parsePlaceHolder(config)">
            <el-option v-for="(opt, i) in config.options" :key="i" :value="opt.value" :label="opt.label">{{ opt.label }}</el-option>
          </el-select>
          <el-select
            :key="'select-multi-' + index"
            v-else-if="config.type === 'multi-selection'"
            v-model="state.searchParams[config.prop]"
            clearable
            multiple
            class="fix-search-width"
            :placeholder="parsePlaceHolder(config)">
            <el-option v-for="(opt, i) in config.options" :key="i" :value="opt.value" :label="opt.label">{{ opt.label }}</el-option>
          </el-select>
          <el-date-picker
            :key="'date-picker-' + index"
            v-else-if="config.type === 'daterange' || config.type === 'datetimerange' || config.type === 'date'"
            v-model="state.searchParams[config.prop]"
            :default-time="config.type === 'datetimerange' ? config.defaultTime || ['00:00:00', '23:59:59'] : config.defaultTime"
            :end-placeholder="config.endPlaceholder || '结束日期'"
            :picker-options="config.pickerOptions || {}"
            :placeholder="config.placeholder || `请选择${config.name}`"
            :start-placeholder="config.startPlaceholder || '开始日期'"
            :type="config.type"
            clearable
            range-separator="~"
            size="small"
            class="fix-search-width"
            unlink-panels
            value-format="timestamp"></el-date-picker>
          <el-switch :key="'switch-' + index" v-else-if="config.type === 'switch'" v-model="state.searchParams[config.prop]"></el-switch>
        </el-form-item>
        <slot name="search-form-item-after"></slot>
        <el-form-item>
          <el-button @click="reset">{{ $t('RESET_BTN') }}</el-button>
          <el-button type="primary" @click="search">{{ $t('SEARCH_BTN') }}</el-button>
        </el-form-item>
      </el-form>
      <slot name="after-search"></slot>
      <div style="flex: auto"></div>
      <slot name="right-extra"></slot>
    </div>
    <el-table
      ref="tableRef"
      :data="state.data"
      v-loading="state.loading"
      :highlight-current-row="highlightCurrentRow"
      :current-row-key="currentRowKey"
      @current-change="(newRow, oldRow) => emit('current-change', newRow, oldRow)"
      :height="height"
      :max-height="maxHeight">
      <slot></slot>
    </el-table>
    <!-- 表格分页: 开始 -->
    <div v-if="props.showPagination" class="table-pagination">
      <div style="flex: auto"></div>
      <el-pagination
        background
        small="small"
        v-model:current-page="state.page"
        v-model:page-size="state.limit"
        :total="state.totalCount"
        :layout="props.pageLayout"
        @size-change="handlePageSizeChange"
        @current-change="changePage"
        :page-sizes="props.pageSizes"></el-pagination>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onMounted, reactive, ref, watch, type PropType } from 'vue';
import { useRoute, useRouter, type LocationQueryValue } from 'vue-router';
import StringUtil from '@/common/StringUtil';
import CommonUtils from '@/common/CommonUtils';
import type { SearchConfig } from '@/types';
import type { ElTable } from 'element-plus';

const props = defineProps({
  showPagination: { type: Boolean, default: true },
  totalCount: { type: Number, default: 0 },
  height: { type: [String, Number], required: false },
  maxHeight: { type: [String, Number], required: false },
  currentRowKey: { type: String, default: '' },
  pageLayout: {
    type: String,
    default: 'total, prev, pager, next, sizes, jumper',
  },
  highlightCurrentRow: { type: Boolean, default: false },
  dataSource: { type: [Array, Function], default: [] as any[] },
  clientPage: { type: Boolean, default: false },
  pageSizes: {
    type: Array as PropType<number[]>,
    default: [5, 10, 20, 50, 100] as number[],
  },
  searchConfig: { type: [Array<SearchConfig>], required: false },
});
const emit = defineEmits<{
  (e: 'update:totalCount', value: number): void;
  (e: 'reset-search'): void;
  (e: 'current-change', currentRow: any, oldCurrentRow: any): void;
}>();
const route = useRoute();
const router = useRouter();

const tableRef = ref<InstanceType<typeof ElTable>>();

const state = reactive({
  name: '',
  loading: false,
  data: [] as any[],
  totalCount: 0,
  page: 1,
  limit: 10,
  searchParams: {} as any,
});

const parsePlaceHolder = (config: SearchConfig): string => {
  if (config.placeholder) {
    return config.placeholder;
  }
  if (config.name) {
    return CommonUtils.translate(config.name as string);
  }
  return '请输入关键字查询';
};

const getSearchConfig = (): SearchConfig[] => {
  return props.searchConfig as SearchConfig[];
};

const reset = () => {
  emit('reset-search');
  if (state.searchParams) {
    for (const key in state.searchParams) {
      state.searchParams[key] = '';
    }
  }
  search();
};
const search = () => {
  const searchParams = { ...state.searchParams };
  for (const key in searchParams) {
    const value = searchParams[key];
    if ('object' === typeof value) {
      // 对象或数组类型
      searchParams[key] = JSON.stringify(value);
    }
  }
  const query = { ...route.query, ...searchParams };
  let changed = false;
  for (const key in query) {
    const value = query[key];
    if (value !== route.query[key]) {
      changed = true;
      break;
    }
  }
  if (changed) {
    router.push({ path: route.path, query });
  } else {
    updateDataList();
  }
};

const updateDataList = () => {
  if (props.dataSource instanceof Array) {
    if (props.clientPage) {
      const start = state.page * state.limit;
      if (start > props.dataSource.length) {
        state.data = [];
      }
      const end = Math.min(start + state.limit, props.dataSource.length);
      state.data = props.dataSource?.slice(start, end) as [];
      state.totalCount = props.dataSource.length;
      emit('update:totalCount', props.dataSource.length);
      return;
    }
    state.data = props.dataSource as [];
    state.totalCount = props.totalCount;
  } else if ('function' === typeof props.dataSource) {
    const func = props.dataSource as Function;
    const page = state.page - 1;
    const limit = state.limit;
    const query = { ...route.query, page, limit };
    state.loading = true;
    const promise = func(query) as Promise<any>;
    promise
      .then(result => {
        state.data = result?.rows || [];
        const totalCount = result?.total;
        state.totalCount = totalCount;
        emit('update:totalCount', totalCount);
      })
      .finally(() => (state.loading = false));
  } else {
    console.warn('未知的dataSource类型', props.dataSource);
  }
};

function parseQueryValue(query: { [p: string]: string | null | LocationQueryValue[] }, key: string): any {
  let value: any = query[key];
  if ('string' === typeof value) {
    if (value.startsWith('[') || value.startsWith('{')) {
      try {
        value = JSON.parse(value);
        query[key] = value;
      } catch (e) {
        console.error('解析json失败', value, query, e);
      }
    } else {
      if (StringUtil.isInt(value)) {
        value = Number.parseInt(value);
      } else if (StringUtil.isFloat(value)) {
        value = Number.parseFloat(value);
      }
    }
  }
  return value;
}

function updateSearchParam() {
  const query = { ...route.query };
  const page = query.page;
  const limit = query.limit;
  state.page = 'string' === typeof page && StringUtil.isNumber(page) ? parseInt(page) : 1;
  state.limit = 'string' === typeof limit && StringUtil.isNumber(limit) ? parseInt(limit) : 10;
  delete query.page;
  delete query.limit;
  const searchConfig = props.searchConfig || [];
  const searchParams = {} as any;
  for (const key in query) {
    if (searchConfig.findIndex(conf => key === conf.prop) < 0) {
      continue;
    }
    searchParams[key] = parseQueryValue(query, key);
  }
  state.searchParams = searchParams;
}

function refresh() {
  updateSearchParam();
  nextTick(updateDataList);
}

function handlePageSizeChange(limit: number) {
  router.push({ path: route.path, query: { ...route.query, limit } });
}

function changePage(page: number) {
  router.push({ path: route.path, query: { ...route.query, page } });
}

watch(
  () => [route.name, route.query, route.params],
  newValue => {
    if (newValue[0] == state.name) {
      refresh();
    } else {
      updateSearchParam();
    }
  }
);

watch(() => props.dataSource, refresh);

function getSelectRow() {
  return tableRef.value?.getSelectionRows();
}

defineExpose({
  refresh,
  getSelectRow,
});

onMounted(() => {
  state.name = route.name as string;
  refresh();
});
</script>

<style lang="less" scoped>
@import '@/assets/main.less';
.table-header {
  display: flex;
}
.fix-search-width {
  width: 190px;
}
.table-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 20px;
  .demonstration,
  .el-pagination,
  .el-pagination__sizes,
  .el-pagination__jump {
    font-size: 13px;
  }
  .demonstration,
  .el-pagination {
    padding: 0;
    margin-bottom: 15px;
  }
  .el-pagination .btn-prev:first-child {
    margin-left: 0;
  }
  .demonstration {
    margin-right: 25px;
    .total {
      font-size: 12px;
      margin: 0 3px;
    }
  }
  .el-pagination.is-background .el-pager li,
  .el-pagination.is-background .btn-prev,
  .el-pagination.is-background .btn-next {
    background-color: white;
    font-family: PingFangSC-Regular;
    font-size: 12px;
    text-align: center;
    border-radius: 4px;
    &.active {
      color: white;
    }
  }
  .el-pagination.is-background .btn-prev:not(:disabled):hover,
  .el-pagination.is-background .btn-next:not(:disabled):hover {
    color: #409eff;
  }
  .el-pagination.is-background .btn-prev:disabled,
  .el-pagination.is-background .btn-next:disabled {
    background-color: #f4f4f5;
    color: #c0c4cc;
  }
  .el-pagination.is-background .el-pager li.more.btn-quicknext {
    border: none;
    margin: 0;
  }
  .el-pagination.is-background .el-pager li:not(.disabled).active {
    background-color: var(--el-pagination-bg-color);
    border-color: var(--el-border-color);
  }
  .el-pagination__editor.el-input {
    width: 34px;
  }
  .el-pagination__jump {
    margin-left: 0;
  }
}
</style>
