<template>
  <div class="version-title" v-show="store.version">
    <el-button link icon="Service" type="primary" @click="state.upgradeDialog = true">{{ $t('UPGRADE') }}</el-button>
    <el-button link icon="Service" type="primary" @click="state.dialog = true">{{ $t('HELP') }}</el-button>
    <el-dialog v-model="state.dialog" :title="$t('HELP')" width="680px">
      <el-form label-suffix=":" label-width="auto">
        <el-form-item :label="$t('SYS_VER')">
          <span>v{{ store.version + (store.inDocker ? '(Docker)' : '') }}</span>
        </el-form-item>
        <el-form-item :label="$t('CLUSTER_MODE')">
          <span>{{ store.host ? $t('YES') : $t('NO') }}</span>
        </el-form-item>
        <el-form-item :label="$t('MACHINE_CODE')">
          <span>{{ store.machineCode }}</span>
        </el-form-item>
        <el-form-item :label="$t('MENU_DOCS')">
          <el-link :href="DOCS_URL" type="primary" target="_blank">{{ DOCS_URL }}</el-link>
        </el-form-item>
        <el-form-item label="API">
          <el-link :href="getApiUrl()" type="primary" target="_blank">{{ getApiUrl() }}</el-link>
        </el-form-item>
        <el-form-item :label="$t('CLI_DOWNLOAD')">
          <el-link :href="state.cliDownloadUrl" type="primary" target="_blank">client-tools.zip</el-link>
        </el-form-item>
        <el-form-item :label="$t('OS')">
          <span>{{ store.os }}</span>
        </el-form-item>
        <el-form-item label="JAVA">
          <span>{{ store.jdk }}</span>
        </el-form-item>
        <el-form-item v-if="store.dev" label="DEV">
          <span>{{ store.dev ? $t('YES') : $t('NO') }}</span>
        </el-form-item>
        <el-form-item label="Git">
          <div>
            <div><el-link href="https://github.com/majianzheng/jarboot">https://github.com/majianzheng/jarboot</el-link></div>
            <div><el-link href="https://gitee.com/majz0908/jarboot">https://gitee.com/majz0908/jarboot</el-link></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="state.dialog = false">{{ $t('CLOSE') }}</el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="state.upgradeDialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :title="$t('UPGRADE')"
      width="680px"
      @closed="clearForm"
      destroy-on-close>
      <el-form label-suffix=":" label-width="auto" :model="state.upgradeForm">
        <el-form-item :label="$t('SYS_VER')">
          <span>v{{ store.version + (store.inDocker ? '(Docker)' : '') }}</span>
        </el-form-item>
        <el-form-item :label="$t('CLUSTER_MODE')">
          <span>{{ store.host ? $t('YES') : $t('NO') }}</span>
        </el-form-item>
        <template v-if="store.inDocker">
          <el-form-item>
            <file-editor :modelValue="dockerUpgradeHelp" readonly name="docker.sh"></file-editor>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item :label="$t('INSTALL_PACKAGE')">
            <el-radio-group v-model="state.upgradeForm.upgradePackage" :disabled="store.upgradeLoading">
              <el-radio :label="$t('PACKAGE_FROM_URL')" :value="0"></el-radio>
              <el-radio :label="$t('PACKAGE_FROM_LOCAL')" :value="1"></el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="state.upgradeForm.upgradePackage === 0" :label="$t('PACKAGE_FROM_URL')">
            <el-input v-model="state.upgradeForm.url" :disabled="store.upgradeLoading"></el-input>
          </el-form-item>
          <el-form-item v-if="state.upgradeForm.upgradePackage === 1" :label="$t('PACKAGE_FROM_LOCAL')">
            <el-upload
              drag
              ref="uploadRef"
              action="/api/jarboot/upgrade/upload"
              v-model:file-list="state.upgradeForm.file"
              accept=".zip"
              :limit="1"
              :disabled="store.upgradeLoading"
              :auto-upload="false">
              <icon-pro icon="UploadFilled" class="el-icon--upload"></icon-pro>
              <div class="el-upload__text">Drop file here or <em>click to upload</em></div>
              <template #tip>
                <div class="el-upload__tip">zip file with a size less than 500MB</div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <div>
              <div>GitHub: https://github.com/majianzheng/jarboot/releases</div>
              <div>Gitee: https://gitee.com/majz0908/jarboot/releases</div>
            </div>
          </el-form-item>
          <el-form-item v-if="store.upgradeLoading">
            <el-text type="warning">{{ $t('UPGRADE_TIPS') }}</el-text>
          </el-form-item>
          <el-form-item v-if="(state.process?.action ?? -1) >= 0">
            <el-text type="info">{{ formatAction() }} : {{ state.process.msg }}</el-text>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="state.upgradeDialog = false" :loading="store.upgradeLoading">{{ $t('CLOSE') }}</el-button>
        <el-button v-if="!store.inDocker" type="primary" :loading="store.upgradeLoading" @click="upgrade">{{ $t('UPGRADE') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useBasicStore } from '@/stores';
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { DOCS_URL } from '@/common/CommonConst';
import { ElMessageBox, type UploadInstance, type UploadUserFile } from 'element-plus';
import CommonNotice from '@/common/CommonNotice';
import { WsManager } from '@/common/WsManager';
import { MSG_EVENT } from '@/common/EventConst';
import type { MsgData } from '@/types';
import CommonUtils from '@/common/CommonUtils';

type UpgradeProgress = {
  action: number;
  msg: string;
};

const store = useBasicStore();
const state = reactive({
  dialog: false,
  upgradeDialog: false,
  cliDownloadUrl: `${window.location.protocol}//${window.location.host}/api/jarboot/public/serverRuntime/client-tools.zip`,
  upgradeForm: {
    upgradePackage: 0,
    url: '',
    file: [] as UploadUserFile[],
  },
  process: {} as UpgradeProgress,
});

const dockerUpgradeHelp = `# 从gitee clone
git clone https://gitee.com/majz0908/jarboot.git
# 从github clone
git clone https://github.com/majianzheng/jarboot.git

cd jarboot

# 编译打包项目，需jdk17+、maven、nodejs16+
mvn clean install -P prod

cd docker

# 构建jarboot镜像
sudo bash docker_image_build.sh

#  初始化docker目录
sudo bash init_docker_dir.sh

# vi .env文件，可通过修改环境变量配置来修改默认的用户名和密码，默认用户名：jarboot 密码：jarboot

# 启动jarboot docker compose，单机版可指定使用【docker-compose-standalone.yml】文件
sudo docker compose up -d
`;

const uploadRef = ref<UploadInstance>();

function getApiUrl(): string {
  if (store.dev) {
    return `${window.location.protocol}//${window.location.hostname}:9899/index.html`;
  }
  return `${window.location.protocol}//${window.location.host}/api-doc/index.html`;
}

function clearForm() {
  state.upgradeForm.url = '';
  state.upgradeForm.file = [];
}

function upgrade() {
  if (1 === state.upgradeForm.upgradePackage) {
    if (state.upgradeForm.file.length < 1) {
      CommonNotice.warn('请选择升级包');
      return;
    }
    store.upgradeLoading = true;
    uploadRef.value?.submit();
  }
}

function upgradeProgress(data: MsgData) {
  const progress = JSON.parse(data.body) as UpgradeProgress;
  state.process = progress;
  if (progress.action < 0) {
    console.info('升级失败:', progress.msg);
    store.upgradeLoading = false;
    clearForm();
    CommonNotice.error(progress.msg);
    return;
  }
  if (!store.upgradeLoading) {
    CommonNotice.success('系统正在升级：' + state.process.msg);
    store.upgradeLoading = true;
  }
}

function formatAction() {
  // 0 文件解压缩 1 文件检验 2 开始升级 -1 升级失败
  switch (state.process.action) {
    case 0:
      return '初始化安装包';
    case 1:
      return '文件检验';
    case 2:
      return '开始升级';
    default:
      return '升级失败';
  }
}

onMounted(() => {
  console.info('version components mounted');
  WsManager.addMessageHandler(MSG_EVENT.UPGRADE_PROGRESS, upgradeProgress);
  WsManager.addReconnectSuccessHandler('upgrade', () => {
    console.info('reconnected success! upgrade finished.');
    if (store.upgradeLoading || state.process?.action >= 0) {
      console.info('升级完成，系统重新连接成功！');
      store.upgradeLoading = false;
      state.upgradeDialog = false;
      setTimeout(() => {
        window.location.reload();
      }, 5000);
      ElMessageBox.confirm('升级完成！将在5秒钟后刷新页面！', CommonUtils.translate('WARN'), {});
    }
  });
});

onUnmounted(() => {
  WsManager.removeMessageHandler(MSG_EVENT.UPGRADE_PROGRESS);
  WsManager.removeReconnectSuccessHandler('upgrade');
});
</script>

<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
