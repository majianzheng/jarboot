<template>
  <div>
    <canvas class="bg-canvas" ref="bgRef"></canvas>
    <div class="login-top-header">
      <img alt="Jarboot logo" class="logo" :src="logoUrl" />
      <div class="header-right">
        <div class="header-tools">
          <div class="menu-button">
            <jarboot-version></jarboot-version>
          </div>
          <div class="menu-button">
            <el-button size="small" link @click="openDoc">{{ $t('MENU_DOCS') }}</el-button>
          </div>
          <div class="menu-button">
            <theme-switch></theme-switch>
          </div>
          <div class="menu-button">
            <language-switch></language-switch>
          </div>
        </div>
      </div>
    </div>
    <div v-if="basic.mobileDevice" class="login-form">
      <div class="internal-sys-tip">
        <div>{{ $t('INTERNAL_SYS_TIP') }}</div>
        <div>{{ $t('INTERNAL_SYS_TIP1') }}</div>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" status-icon :rules="rules" label-width="0" size="large">
        <el-form-item label="" prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="User"
            :placeholder="$t('USER_NAME')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            autocomplete="off" />
        </el-form-item>
        <el-form-item label="" prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="Lock"
            :placeholder="$t('PASSWORD')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            show-password
            type="password"
            autocomplete="off" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" class="login-button" type="primary" @click="submitForm(loginFormRef)">
            {{ $t('LOGIN') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-else class="login-form">
      <div class="login-header">{{ $t('LOGIN') }}</div>
      <div class="internal-sys-tip">
        <div>{{ $t('INTERNAL_SYS_TIP') }}</div>
        <div>{{ $t('INTERNAL_SYS_TIP1') }}</div>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" status-icon :rules="rules" label-width="0" size="large">
        <el-form-item label="" prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="User"
            :placeholder="$t('USER_NAME')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            autocomplete="off" />
        </el-form-item>
        <el-form-item label="" prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="Lock"
            :placeholder="$t('PASSWORD')"
            @keydown.enter="submitForm(loginFormRef)"
            clearable
            show-password
            type="password"
            autocomplete="off" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" class="login-button" type="primary" @click="submitForm(loginFormRef)">
            {{ $t('LOGIN') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import type { FormInstance } from 'element-plus';
import CommonUtils from '@/common/CommonUtils';
import { useBasicStore, useUserStore } from '@/stores';
import { BG_URL, DOCS_URL, LOGO_URL } from '@/common/CommonConst';

const loginFormRef = ref<FormInstance>();
const bgRef = ref();
const basic = useBasicStore();

const loginForm = reactive({
  username: '',
  password: '',
});
const logoUrl = LOGO_URL;

const userStore = useUserStore();
const rules = reactive({
  username: [{ required: true, message: CommonUtils.translate('INPUT_USERNAME'), trigger: 'blur' }],
  password: [{ required: true, message: CommonUtils.translate('INPUT_PASSWORD'), trigger: 'blur' }],
});
const loading = ref(false);
let bgAni = null as any;

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  const valid = await formEl.validate();
  if (!valid) {
    console.log('error submit!');
    return false;
  }
  loading.value = true;
  await userStore.login(loginForm.username, loginForm.password);
  loading.value = false;
};
const openDoc = () => window.open(DOCS_URL);

function drawImage(ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    let flag = true;
    img.onload = () => {
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      resolve(true);
      flag = false;
    };
    img.onerror = () => {
      console.log('error load image!');
      resolve(false);
      flag = false;
    };
    setTimeout(() => {
      if (flag) {
        console.info('load image timeout');
        reject('load image timeout');
      }
    }, 15000);
    img.src = BG_URL;
  });
}

onMounted(async () => {
  const canvas = bgRef.value;
  const ctx = canvas.getContext('2d');

  canvas.height = window.innerHeight;
  canvas.width = window.innerWidth;
  window.document.body.style.overflow = 'hidden';

  const hasImage = await drawImage(ctx, canvas);
  if (hasImage) {
    console.log('load image success!');
    return;
  }
  const texts = '0123456789ABCDE'.split('');

  const fontSize = 14;
  const columns = canvas.width / fontSize;
  // 用于计算输出文字时坐标，所以长度即为列数
  const drops = [] as number[];
  //初始值
  for (let x = 0; x < columns; x++) {
    drops[x] = 1;
  }

  function draw() {
    //让背景逐渐由透明到不透明
    ctx.fillStyle = 'rgba(0, 0, 0, 0.05)';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    //文字颜色
    ctx.fillStyle = 'rgb(37,188,36)';
    ctx.font = fontSize + 'px arial';
    //逐行输出文字
    for (let i = 0; i < drops.length; i++) {
      const text = texts[Math.floor(Math.random() * texts.length)];
      ctx.fillText(text, i * fontSize, drops[i] * fontSize);

      if (drops[i] * fontSize > canvas.height || Math.random() > 0.95) {
        drops[i] = 0;
      }

      drops[i]++;
    }
  }

  bgAni = setInterval(draw, 200);
});
onUnmounted(() => bgAni && clearInterval(bgAni));
</script>
<style lang="less">
body {
  margin: 0 !important;
  padding: 0 !important;
  background: transparent !important;
}
.login-top-header {
  display: flex;
  height: 50px;
  border-bottom: 1px solid rgb(76, 77, 79);
  background: var(--el-fill-color-darker);
  padding-left: 15px;
  opacity: 0.82;
  .logo {
    height: 38px;
    margin: 6px;
  }
  .header-right {
    display: flex;
    flex: auto;
    justify-content: right;
  }
  .header-tools {
    display: flex;
    .menu-button {
      margin: auto 10px;
    }
  }
}

.bg-canvas {
  position: absolute;
  margin: 0;
  z-index: -1;
  overflow: hidden;
}
.login-form {
  width: 30%;
  position: absolute;
  right: 30px;
  top: 10%;
  .login-header {
    width: 100%;
    line-height: 45px;
    font-size: 32px;
    margin-top: 58px;
    text-align: center;
    color: #e9e9eb;
  }
  .internal-sys-tip {
    width: 100%;
    line-height: 25px;
    font-size: 20px;
    margin: 25px 0 20px 0;
    text-align: center;
    font-weight: 800;
    color: rgba(255, 0, 0, 0.8);
  }
  .login-button {
    width: 100%;
  }
}
@media screen and (max-width: 850px) {
  .login-form {
    width: 80%;
    position: absolute;
    right: 10%;
    top: 20%;
  }
}
</style>
