<template>
  <div class="basic-config-main">
    <el-form label-width="auto" label-suffix=":">
      <el-form-item :label="$t('SYS_LOGO')">
        <el-upload
          ref="logoUploadRef"
          :action="LOGO_URL"
          :show-file-list="false"
          v-model:file-list="formData.logoFile"
          accept="image/png"
          :on-success="uploadLogoSuccess"
          :on-error="uploadLogoClear"
          :limit="1"
          :auto-upload="false">
          <div class="config-upload-icon">
            <icon-pro icon="Plus"></icon-pro>
            <div class="config-upload-text">{{ $t('UPLOAD_BUTTON') }}</div>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item :label="$t('SYS_LOGO_PREVIEW')">
        <div style="display: flex">
          <el-image alt="Jarboot logo" fit="cover" class="logo" :preview-src-list="[formData.logoUrl]" :src="formData.logoUrl"></el-image>
          <div>
            <a style="margin-left: 10px" :href="formData.logoUrl" download>{{ $t('DOWNLOAD') }}</a>
          </div>
        </div>
      </el-form-item>
      <el-form-item :label="$t('LOGIN_BG')">
        <el-upload
          ref="bgUploadRef"
          :action="BG_URL"
          :show-file-list="false"
          v-model:file-list="formData.bgFile"
          accept="image/png"
          :on-success="uploadBgSuccess"
          :on-error="uploadBgClear"
          :limit="1"
          :auto-upload="false">
          <div class="config-upload-icon">
            <icon-pro icon="Plus"></icon-pro>
            <div class="config-upload-text">{{ $t('UPLOAD_BUTTON') }}</div>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item :label="$t('LOGIN_BG_PREVIEW')">
        <div style="display: flex">
          <el-image alt="Jarboot bg" style="height: 80px" :preview-src-list="[formData.bgUrl]" :src="formData.bgUrl" @load="bgLoaded">
            <template #error>
              <div class="image-slot">
                <icon-pro icon="Picture"></icon-pro>
              </div>
            </template>
          </el-image>
          <div v-if="formData.hasBg" style="padding-left: 10px">
            <div>
              <a :href="formData.bgUrl" download>{{ $t('DOWNLOAD') }}</a>
            </div>
            <div v-if="formData.bgUrl.endsWith('.png')">
              <el-button link type="danger" @click="deleteBgImage">{{ $t('DELETE') }}</el-button>
            </div>
          </div>
        </div>
      </el-form-item>
      <el-form-item :label="$t('SYS_ICON')">
        <el-upload
          ref="iconUploadRef"
          :action="FAVICON_URL"
          :show-file-list="false"
          v-model:file-list="formData.iconFile"
          accept="image/x-icon,image/png"
          :on-success="uploadIconSuccess"
          :on-error="uploadIconClear"
          :limit="1"
          :auto-upload="false">
          <div class="config-upload-icon">
            <icon-pro icon="Plus"></icon-pro>
            <div class="config-upload-text">{{ $t('UPLOAD_BUTTON') }}</div>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item :label="$t('SYS_ICON_PREVIEW')">
        <div style="display: flex">
          <img alt="icon" height="26" :src="formData.iconUrl" />
          <div>
            <a style="margin-left: 10px" :href="formData.iconUrl" download>{{ $t('DOWNLOAD') }}</a>
          </div>
        </div>
      </el-form-item>
      <el-form-item :label="$t('SYS_NAME')">
        <el-input v-model="formData.productName"></el-input>
      </el-form-item>
    </el-form>
    <div class="foot-buttons">
      <el-button :loading="formData.loading" @click="save" type="primary">{{ $t('SAVE') }}</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, watch } from 'vue';
import type { UploadInstance, UploadUserFile } from 'element-plus';
import { useBasicStore } from '@/stores';
import CommonNotice from '@/common/CommonNotice';
import StringUtil from '@/common/StringUtil';
import { BG_URL, FAVICON_URL, LOGO_URL } from '@/common/CommonConst';
import Request from '@/common/Request';

const logoUploadRef = ref<UploadInstance>();
const iconUploadRef = ref<UploadInstance>();
const bgUploadRef = ref<UploadInstance>();

const basic = useBasicStore();
const formData = reactive({
  loading: false,
  logoFile: [] as UploadUserFile[],
  iconFile: [] as UploadUserFile[],
  bgFile: [] as UploadUserFile[],
  productName: 'Jarboot',
  iconUrl: FAVICON_URL,
  logoUrl: LOGO_URL,
  bgUrl: BG_URL,
  hasBg: false,
});

watch(
  () => formData.iconFile,
  (newIcon: UploadUserFile[]) => {
    if (newIcon?.length <= 0) {
      return;
    }
    let reader = new FileReader();
    reader.onload = (res: ProgressEvent<FileReader>) => {
      formData.iconUrl = (res?.target?.result as string) || FAVICON_URL;
    };
    reader.readAsDataURL(newIcon[newIcon.length - 1]?.raw as Blob);
  }
);
watch(
  () => formData.logoFile,
  (logoFile: UploadUserFile[]) => {
    if (logoFile?.length <= 0) {
      return;
    }
    let reader = new FileReader();
    reader.onload = (res: ProgressEvent<FileReader>) => {
      formData.logoUrl = (res?.target?.result as string) || LOGO_URL;
    };
    reader.readAsDataURL(logoFile[logoFile.length - 1]?.raw as Blob);
  }
);
watch(
  () => formData.bgFile,
  (bgFile: UploadUserFile[]) => {
    if (bgFile?.length <= 0) {
      return;
    }
    let reader = new FileReader();
    reader.onload = (res: ProgressEvent<FileReader>) => {
      formData.bgUrl = (res?.target?.result as string) || BG_URL;
    };
    reader.readAsDataURL(bgFile[bgFile.length - 1]?.raw as Blob);
  }
);

function bgLoaded() {
  formData.hasBg = true;
}

function updateImage() {
  const img = document.querySelector('img.logo') as HTMLImageElement;
  const src = img.src;
  const i = src.lastIndexOf('?');
  img.src = '';
  if (i > 0) {
    img.src = src.substring(0, i) + `?t=${Date.now()}`;
  } else {
    img.src = `${src}?t=${Date.now()}`;
  }

  const iconEle = document.head.querySelector('link[rel="icon"]') as HTMLLinkElement;
  const index = iconEle.href.lastIndexOf('?');
  if (index > 0) {
    iconEle.href = iconEle.href.substring(0, index) + `?t=${Date.now()}`;
  } else {
    iconEle.href = `${iconEle.href}?t=${Date.now()}`;
  }
}

const uploadLogoClear = () => {
  logoUploadRef.value?.clearFiles();
};
const uploadIconClear = () => {
  iconUploadRef.value?.clearFiles();
};
const uploadBgClear = () => {
  bgUploadRef.value?.clearFiles();
};

const LOGO_KEY = 'logo';
const ICON_KEY = 'icon';
const BG_KEY = 'bg';

let uploadSuccessMap = new Map<string, boolean>();
function uploadLogoSuccess() {
  uploadLogoClear();
  uploadSuccessMap.set(LOGO_KEY, true);
  formData.logoUrl = LOGO_URL;
}
function uploadIconSuccess() {
  uploadIconClear();
  uploadSuccessMap.set(ICON_KEY, true);
  formData.iconUrl = FAVICON_URL;
}
function uploadBgSuccess() {
  uploadBgClear();
  uploadSuccessMap.set(BG_KEY, true);
  formData.bgUrl = BG_URL;
}

function waitUploadFinished() {
  const fd = setInterval(() => {
    let isAllUploaded = true;
    uploadSuccessMap.forEach((value, key) => {
      if (key && !value) {
        // 有一个未完成即判定为未完成
        isAllUploaded = false;
      }
    });
    if (isAllUploaded) {
      // 停止
      clearInterval(fd);
      CommonNotice.success('保存成功');
      setTimeout(updateImage, 100);
    }
  }, 200);
}

async function deleteBgImage() {
  if (formData.hasBg) {
    await Request.delete('/jarboot/preferences/image/bg.png', {});
    formData.bgUrl = BG_URL;
    formData.hasBg = false;
    CommonNotice.success('删除成功');
  }
}

function save() {
  formData.loading = true;
  try {
    if (StringUtil.isEmpty(formData.productName)) {
      CommonNotice.warn('产品名称不可为空');
      return;
    }
    // 清理历史上传文件
    formData.logoFile = formData.logoFile.slice(-1);
    formData.iconFile = formData.iconFile.slice(-1);
    if (formData.logoFile.length) {
      uploadSuccessMap.set(LOGO_KEY, false);
      logoUploadRef.value?.submit();
    }
    if (formData.iconFile.length) {
      uploadSuccessMap.set(ICON_KEY, false);
      iconUploadRef.value?.submit();
    }
    if (formData.bgFile.length) {
      uploadSuccessMap.set(BG_KEY, false);
      bgUploadRef.value?.submit();
    }
    // 保存产品名称
    if (formData.productName !== basic.productName) {
      Request.put(`/jarboot/preferences/productName?productName=${formData.productName}`, {}).then(() => {
        document.title = formData.productName;
        basic.productName = formData.productName;
        0 === uploadSuccessMap.size && CommonNotice.success('保存成功');
      });
    } else {
      0 === uploadSuccessMap.size && CommonNotice.success('产品名未变动，无需保存');
    }
    // 延迟下等待上传完成
    if (uploadSuccessMap.size > 0) {
      waitUploadFinished();
    }
  } finally {
    formData.loading = false;
  }
}

onMounted(async () => {
  basic.productName = await Request.get<string>('/jarboot/preferences/productName', {});
  formData.productName = basic.productName;
});
</script>

<style lang="less" scoped>
.basic-config-main {
  padding: 0 15%;
  height: calc(100vh - 76px);
  overflow: auto;
  color: var(--el-color-text);
  .logo {
    height: 38px;
    margin: 0 15px;
  }
  .config-upload-icon {
    background: var(--el-bg-color);
    border: 1px dashed var(--el-color-primary);
    box-sizing: border-box;
    width: 120px;
    height: 60px;
    border-radius: 2px;
    padding: 5px;
    text-align: center;
    .config-upload-text {
      position: relative;
      font-size: 14px;
      top: -15px;
    }
  }
  .foot-buttons {
    width: 100%;
    bottom: 20px;
    text-align: center;
  }

  .image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 150px;
    height: 80px;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-secondary);
    font-size: 30px;
  }
  .image-slot .el-icon {
    font-size: 30px;
  }
}
</style>
