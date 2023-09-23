<template>
  <el-popover :visible="state.cronPopover" width="700px">
    <cron-editor :cron-value="state.cron" @change="changeCron" @close="togglePopover(false)" max-height="400px" :i18n="locale"></cron-editor>
    <template #reference>
      <el-input @click="togglePopover(true)" v-model="state.cron" placeholder="* * * * * ? *"></el-input>
    </template>
  </el-popover>
</template>

<script lang="ts" setup>
import { onMounted, reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';

const { locale } = useI18n();

type CronInputProp = {
  modelValue: string;
};

const props = defineProps<CronInputProp>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const state = reactive({
  cronPopover: false,
  cron: '',
});
const changeCron = (val?: string) => {
  if (typeof val !== 'string') return false;
  state.cron = val;
  emit('update:modelValue', val);
};
const togglePopover = (bol: boolean) => {
  state.cronPopover = bol;
};
watch(
  () => props.modelValue,
  newValue => {
    state.cron = newValue;
  }
);
onMounted(() => {
  if (props.modelValue) {
    state.cron = props.modelValue;
  }
});
</script>
