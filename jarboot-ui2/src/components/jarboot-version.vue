<template>
  <div class="version-title" v-show="store.version">
    <el-button link type="warning" @click="state.upgradeDialog = true">
      <template #icon><icon-pro icon="icon-upgrade" size="18px"></icon-pro></template>
      {{ $t('UPGRADE') }}
    </el-button>
    <el-button link icon="Service" type="primary" @click="state.dialog = true">{{ $t('HELP') }}</el-button>
    <el-dialog v-model="state.dialog" :title="$t('HELP')" width="680px">
      <el-form label-suffix=":" label-width="auto">
        <el-form-item :label="$t('SYS_VER')">
          <span>{{ formatVerTitle() }}</span>
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
      :show-close="false"
      :title="$t('UPGRADE_OR_RESET')"
      width="680px"
      @closed="clearForm"
      destroy-on-close>
      <el-form label-suffix=":" label-width="auto" :model="state.upgradeForm">
        <el-form-item :label="$t('SYS_VER')">
          <span>{{ formatVerTitle() }}</span>
        </el-form-item>
        <template v-if="store.inDocker">
          <el-form-item>
            <div style="width: 100%">
              <div>Docker jarboot image build and deploy script:</div>
              <file-editor :modelValue="dockerUpgradeHelp" readonly name="docker.sh"></file-editor>
            </div>
          </el-form-item>
          <el-form-item :label="$t('HELP')">
            <div style="width: 100%">
              <a style="margin-right: 15px" target="_blank" href="https://www.yuque.com/jarboot/usage/docker_install">Install docker in Linux</a>
              <a target="_blank" href="https://www.yuque.com/jarboot/usage/use_docker">Docker deploy</a>
            </div>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item :label="$t('INSTALL_PACKAGE')" v-if="!store.upgradeLoading">
            <el-radio-group v-model="state.upgradeForm.upgradePackage" :disabled="store.upgradeLoading">
              <el-radio :label="$t('PACKAGE_FROM_URL')" :value="0"></el-radio>
              <el-radio :label="$t('PACKAGE_FROM_LOCAL')" :value="1"></el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="!store.upgradeLoading && state.upgradeForm.upgradePackage === 0" :label="$t('PACKAGE_FROM_URL')">
            <el-input
              v-model="state.upgradeForm.url"
              placeholder="https:// ... /jarboot/releases/ ......  /jarboot-bin-v*.*.*.zip"
              :disabled="store.upgradeLoading"></el-input>
          </el-form-item>
          <el-form-item v-if="!store.upgradeLoading && state.upgradeForm.upgradePackage === 1" :label="$t('PACKAGE_FROM_LOCAL')">
            <el-upload
              drag
              ref="uploadRef"
              class="upgrade-uploader"
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
          <el-form-item :label="$t('LATEST_VER_DOWNLOAD')">
            <div>
              <div>
                <a target="_blank" href="https://github.com/majianzheng/jarboot/releases">
                  【GitHub】： https://github.com/majianzheng/jarboot/releases
                </a>
              </div>
              <div>
                <a target="_blank" href="https://gitee.com/majz0908/jarboot/releases">
                  【Gitee】： https://gitee.com/majz0908/jarboot/releases
                </a>
              </div>
            </div>
          </el-form-item>
          <el-form-item v-if="store.upgradeLoading">
            <el-text type="warning">{{ $t('UPGRADE_TIPS') }}</el-text>
          </el-form-item>
          <el-form-item v-if="(state.process?.action ?? -1) >= 0">
            <div style="height: 180px; overflow: auto; width: 100%" ref="infoRef">
              <div type="info" v-for="(info, i) in state.processInfo" :key="i">{{ info }}</div>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="state.upgradeDialog = false" :loading="store.upgradeLoading">{{ $t('CLOSE') }}</el-button>
        <el-button v-if="!store.inDocker" type="primary" :loading="store.upgradeLoading" @click="upgrade">
          {{ $t('SUBMIT_BTN') }}
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="state.upgradeSuccessDialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('UPGRADE_OR_RESET')"
      width="600px"
      destroy-on-close>
      <el-result icon="success" title="Success Tip" sub-title="Please follow the instructions">
        <template #sub-title>
          <div>
            <span>{{ $t('SYS_VER') }}: {{ formatVerTitle() }}</span>
          </div>
        </template>
        <template #title>
          <div>
            <el-countdown
              :value="state.reloadTime"
              format="ss"
              @finish="reloadPage"
              :prefix="$t('UPGRADE_SUCCESS_PREFIX_TIP')"
              :suffix="$t('UPGRADE_SUCCESS_SUBFIX_TIP')" />
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="reloadPage">{{ $t('REFRESH_BTN') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useBasicStore } from '@/stores';
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import { DOCS_URL } from '@/common/CommonConst';
import type { UploadInstance, UploadUserFile } from 'element-plus';
import CommonNotice from '@/common/CommonNotice';
import { WsManager } from '@/common/WsManager';
import { MSG_EVENT } from '@/common/EventConst';
import type { MsgData } from '@/types';
import CommonUtils from '@/common/CommonUtils';
import Request from '@/common/Request';

type UpgradeProgress = {
  host: string;
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
  processInfo: [] as string[],
  upgradeSuccessDialog: false,
  reloadTime: Date.now(),
});
const infoRef = ref<HTMLDivElement>();

const dockerUpgradeHelp = `# git clone https://gitee.com/majz0908/jarboot.git #  from gitee clone
# git clone https://github.com/majianzheng/jarboot.git  # from github
cd jarboot
git pull
# 编译打包项目，build and package，jdk17+、maven、nodejs16+
mvn clean install -P prod
cd docker
sudo bash docker_image_build.sh
docker save jarboot:latest -o ./jarboot.img  # save jarboot docker image.
scp jarboot.img root@x.x.x.x:/xxx/  # upload docker image to product env.
# 生产环境，product env
docker load -i jarboot.img
sudo bash init_docker_dir.sh
# vi .env  # modify default username：jarboot password：jarboot
sudo docker compose up -d
# sudo docker compose -f docker-compose-standalone.yml up -d
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

function formatVerTitle() {
  const mode = store.host ? CommonUtils.translate('CLUSTER_MODE') : CommonUtils.translate('STANDALONE');
  const inDocker = store.inDocker ? '(Docker)' : '';
  return `v${store.version} - ${mode} ${inDocker}`;
}

function upgrade() {
  if (1 === state.upgradeForm.upgradePackage) {
    if (state.upgradeForm.file.length < 1) {
      CommonNotice.warn(CommonUtils.translate('SELECT_UPGRADE_PACKAGE'));
      return;
    }
    WsManager.upgrading = true;
    store.upgradeLoading = true;
    uploadRef.value?.submit();
  } else {
    if (state.upgradeForm.url?.length == 0) {
      CommonNotice.warn(CommonUtils.translate('INPUT_UPGRADE_PACKAGE_URL'));
      return;
    }
    WsManager.upgrading = true;
    store.upgradeLoading = true;
    const params = new FormData();
    params.append('url', state.upgradeForm.url);
    Request.post('/api/jarboot/upgrade/url', params);
  }
}

function upgradeProgress(data: MsgData) {
  const progress = JSON.parse(data.body) as UpgradeProgress;
  state.process = progress;
  state.processInfo.push(`${formatAction()}: ${progress.msg}`);
  nextTick(() => {
    if (infoRef.value) {
      infoRef.value.scrollTop = infoRef.value.scrollHeight;
    }
  });
  if (progress.action < 0) {
    console.error('升级失败:', progress.msg);
    store.upgradeLoading = false;
    WsManager.upgrading = false;
    clearForm();
    CommonNotice.error(progress.msg);
    return;
  }
  if (!store.upgradeLoading) {
    CommonNotice.success(CommonUtils.translate('UPGRADE_TIPS'));
    WsManager.upgrading = true;
    store.upgradeLoading = true;
  }
}

function formatAction() {
  // 0 文件解压缩 1 文件检验 2 开始升级 -1 升级失败
  let act = '';
  switch (state.process.action) {
    case 0:
      act = '初始化安装包';
      break;
    case 1:
      act = '文件检验';
      break;
    case 2:
      act = '开始升级';
      break;
    default:
      act = '升级失败';
      break;
  }
  let node = state.process.host ? `[${state.process.host}] ` : '';
  return node + act;
}

async function reconnectHandler() {
  console.info('reconnected success! upgrade finished.');
  if (store.upgradeLoading || state.process?.action >= 0) {
    console.info('升级完成，系统重新连接成功！');
    store.upgradeLoading = false;
    state.upgradeDialog = false;
    WsManager.upgrading = false;
    await store.init();
    state.reloadTime = Date.now() + 10000;
    state.upgradeSuccessDialog = true;
  }
}

function reloadPage() {
  state.upgradeSuccessDialog = false;
  window.location.reload();
}

onMounted(() => {
  WsManager.addMessageHandler(MSG_EVENT.UPGRADE_PROGRESS, upgradeProgress);
  WsManager.addReconnectSuccessHandler('upgrade', reconnectHandler);
});

onUnmounted(() => {
  WsManager.removeMessageHandler(MSG_EVENT.UPGRADE_PROGRESS);
  WsManager.removeReconnectSuccessHandler('upgrade');
});
</script>

<style lang="less">
.upgrade-uploader {
  .el-upload-dragger {
    padding: 10px 50px;
  }
}
</style>
<style scoped>
.version-title {
  color: var(--el-text-color-regular);
  display: inline-block;
  font-size: var(--el-font-size-small);
}
</style>
