<template>
  <div>
    <div class="login-form">
      <div class="login-header">{{$t('LOGIN')}}</div>
      <div class="internal-sys-tip">
        <div>{{$t('INTERNAL_SYS_TIP')}}</div>
        <div>{{$t('INTERNAL_SYS_TIP1')}}</div>
      </div>
      <el-form
          ref="ruleFormRef"
          :model="ruleForm"
          status-icon
          :rules="rules"
          label-width="0"
          size="large"
          class="demo-ruleForm"
      >
        <el-form-item label="" prop="username">
          <el-input v-model="ruleForm.username" :prefix-icon="User" :placeholder="$t('USER_NAME')" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="" prop="password">
          <el-input v-model="ruleForm.password" :prefix-icon="Lock" :placeholder="$t('PASSWORD')" type="password" autocomplete="off" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" class="login-button" type="primary" @click="submitForm(ruleFormRef)">
            {{$t('LOGIN')}}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>

</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { User, Lock } from "@element-plus/icons-vue";
import CommonUtils from "@/common/CommonUtils";
import {useUserStore} from "@/stores";

const ruleFormRef = ref<FormInstance>()

const ruleForm = reactive({
  username: '',
  password: '',
})
const userStore = useUserStore();
const rules = reactive({
  password: [{ required: true, message: CommonUtils.translate('INPUT_USERNAME'), trigger: 'blur' }],
  username: [{ required: true, message: CommonUtils.translate('INPUT_PASSWORD'), trigger: 'blur' }],
});
const loading = ref(false);

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  const valid = await formEl.validate();
  if (!valid) {
    console.log('error submit!');
    return false;
  }
  loading.value = true;
  await userStore.login(ruleForm.username, ruleForm.password);
  loading.value = false;
}
</script>
<style lang="less" scoped>
.login-form {
  width: 30%;
  position: absolute;
  right: 15px;
  top: 10%;
  .login-header {
    width: 100%;
    line-height: 45px;
    font-size: 32px;
    margin-top: 58px;
    text-align: center;
    color: var(--el-text-color-regular);
  }
  .internal-sys-tip {
    width: 100%;
    line-height: 25px;
    font-size: 20px;
    margin: 25px 0 20px 0;
    text-align: center;
    font-weight: 800;
    color: rgba(255,0,0,.8);
  }
  .login-button {
    width: 100%;
  }
}
</style>