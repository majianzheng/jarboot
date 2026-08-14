<template>
  <el-dialog :model-value="envDialog" :title="$t('ENVIRONMENT_VARIABLES')" width="500px" destroy-on-close>
    <div style="margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end">
      <el-button size="small" type="primary" @click="handleAddEnv">{{ $t('CREATE') }}</el-button>
      <el-button size="small" type="default" @click="handleEditEnv" :disabled="selectedEnv === null">{{ $t('MODIFY') }}</el-button>
      <el-button size="small" type="danger" @click="handleDeleteEnv" :disabled="selectedEnv === null">{{ $t('DELETE') }}</el-button>
    </div>
    <el-table
      :data="envVariables"
      height="300"
      style="width: 100%"
      @row-click="handleEnvRowClick"
      :highlight-current-row="true"
      size="small"
      current-row-key="key">
      <el-table-column prop="key" :label="$t('ENV_KEY')" width="180" show-overflow-tooltip></el-table-column>
      <el-table-column prop="value" :label="$t('ENV_VALUE')" show-overflow-tooltip></el-table-column>
      <el-table-column width="80px">
        <template #default="{ $index }">
          <el-button size="small" type="primary" link @click="handleRowEditEnv($index)" icon="Edit"></el-button>
          <el-button size="small" type="danger" link @click="handleRowDeleteEnv($index)" icon="Delete"></el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 10px">
        <el-button @click="emit('update:envDialog', false)">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" @click="saveEnvVariables">{{ $t('CONFIRM') }}</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 环境变量编辑对话框 -->
  <el-dialog v-model="envEditDialogVisible" :title="editingEnvIndex === -1 ? $t('NEW_ENV_VARIABLE') : $t('EDIT_ENV_VARIABLE')" width="400px">
    <el-form :model="editingEnv" label-width="80px" @submit.prevent>
      <el-form-item :label="$t('ENV_KEY')">
        <el-input v-model="editingEnv.key"></el-input>
      </el-form-item>
      <el-form-item :label="$t('ENV_VALUE')">
        <el-input v-model="editingEnv.value"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 10px">
        <el-button @click="envEditDialogVisible = false">{{ $t('CANCEL') }}</el-button>
        <el-button type="primary" @click="confirmEnvEdit">{{ $t('CONFIRM') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { ElMessageBox } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';

// 环境变量相关状态
const envVariables = ref<Array<{ key: string; value: string }>>([]);
const selectedEnv = ref<number | null>(null);
const envEditDialogVisible = ref(false);
const editingEnvIndex = ref(-1);

const props = defineProps<{
  envs?: string[];
  envDialog: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:envDialog', value: boolean): void;
  (e: 'update:envs', value: string[]): void;
}>();

watch(
  () => props.envs,
  newVal => {
    parseEnvVariables(newVal ?? []);
  }
);
const editingEnv = reactive({
  key: '',
  value: '',
});

// 监听环境变量对话框打开事件
watch(
  () => props.envDialog,
  newVal => {
    if (newVal) {
      parseEnvVariables(props.envs ?? []);
      selectedEnv.value = null;
    }
  }
);

// 解析环境变量字符串
function parseEnvVariables(envs: string[]) {
  envVariables.value = [];
  if (!envs?.length) return;

  envs.forEach(pair => {
    const [key, ...valueParts] = pair.split('=');
    const value = valueParts.join('=');
    envVariables.value.push({ key: key.trim(), value: value.trim() });
  });
}

// 格式化环境变量为字符串
function formatEnvVariables(): string[] {
  return envVariables.value.map(env => `${env.key}=${env.value}`);
}

// 添加环境变量
function handleAddEnv() {
  editingEnvIndex.value = -1;
  editingEnv.key = '';
  editingEnv.value = '';
  envEditDialogVisible.value = true;
}

// 编辑环境变量
function handleEditEnv() {
  if (selectedEnv.value === null) return;

  editingEnvIndex.value = selectedEnv.value;
  editingEnv.key = envVariables.value[selectedEnv.value].key;
  editingEnv.value = envVariables.value[selectedEnv.value].value;
  envEditDialogVisible.value = true;
}
function handleRowEditEnv(index: number) {
  editingEnvIndex.value = index;
  editingEnv.key = envVariables.value[index].key;
  editingEnv.value = envVariables.value[index].value;
  envEditDialogVisible.value = true;
}

// 删除环境变量
async function handleDeleteEnv() {
  if (selectedEnv.value === null) return;
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + '?', CommonUtils.translate('WARN'), {});
  envVariables.value.splice(selectedEnv.value, 1);
  selectedEnv.value = null;
}

async function handleRowDeleteEnv(index: number) {
  // 是否删除
  await ElMessageBox.confirm(CommonUtils.translate('DELETE') + '?', CommonUtils.translate('WARN'), {});
  envVariables.value.splice(index, 1);
}

// 确认环境变量编辑
function confirmEnvEdit() {
  if (editingEnv.key.trim() === '') return;

  if (editingEnvIndex.value === -1) {
    // 新增
    envVariables.value.push({ key: editingEnv.key, value: editingEnv.value });
  } else {
    // 修改
    envVariables.value[editingEnvIndex.value] = {
      key: editingEnv.key,
      value: editingEnv.value,
    };
  }

  envEditDialogVisible.value = false;
}

// 保存环境变量
function saveEnvVariables() {
  const env = formatEnvVariables();
  emit('update:envDialog', false);
  emit('update:envs', env);
}

// 表格行点击事件
function handleEnvRowClick(row: any, _column: any, _event: any) {
  const index = envVariables.value.findIndex(env => env === row);
  selectedEnv.value = index;
}
</script>
