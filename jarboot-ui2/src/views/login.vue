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
          <el-button class="login-button" type="primary" @click="submitForm(ruleFormRef)">
            {{$t('LOGIN')}}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>

</template>

<script lang="ts" setup>
import { reactive, ref, getCurrentInstance } from 'vue'
import type { FormInstance } from 'element-plus'
import { User, Lock } from "@element-plus/icons-vue";
import router from "@/router";
import OAuthService from "@/services/OAuthService";
import CommonNotice from "@/common/CommonNotice";
import CommonUtils from "@/common/CommonUtils";

const ruleFormRef = ref<FormInstance>()

const ruleForm = reactive({
  username: '',
  password: '',
})
const t = getCurrentInstance()?.appContext.config.globalProperties.$t;
const rules = reactive({
  password: [{ required: true, message: t && t('INPUT_USERNAME'), trigger: 'blur' }],
  username: [{ required: true, message: t && t('INPUT_PASSWORD'), trigger: 'blur' }],
});

const submitForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (!valid) {
      console.log('error submit!');
      return false;
    }
    OAuthService.login(ruleForm.username, ruleForm.password).then(resp => {
      if (resp.resultCode !== 0) {
        CommonNotice.errorFormatted(resp);
        return;
      }
      const user: any = resp.result;
      CommonUtils.storeToken(user.accessToken);
      router.push('/')
    }).catch(CommonNotice.errorFormatted);
  })
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