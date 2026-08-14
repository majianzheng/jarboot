import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from '@/services/OAuthService';
import CommonUtils from '@/common/CommonUtils';
import router from '@/router';
import ClusterManager from '@/services/ClusterManager';
import type { ServiceInstance, JvmProcess, ServerRuntimeInfo, UploadFileInfo, MenuItem } from '@/types';
import { PAGE_LOGIN } from '@/common/route-name-constants';
import PrivilegeService from '@/services/PrivilegeService';
import {
  ATTACHED,
  ATTACHING,
  DEFAULT_PRIVILEGE,
  EXITED,
  NOT_TRUSTED,
  SCHEDULE_TASK_RUNNING,
  STATUS_ATTACHED,
  STATUS_NOT_ATTACHED,
  STATUS_SCHEDULING,
  STATUS_STARTED,
  STATUS_STARTING,
  STATUS_STOPPED,
  STATUS_STOPPING,
  TRUSTED,
} from '@/common/CommonConst';
import UserService from '@/services/UserService';
import { PUB_TOPIC, pubsub } from '@/views/services/ServerPubsubImpl';
import Logger from '@/common/Logger';
import Request from '@/common/Request';
import { CONSOLE_TOPIC } from '@/types';
import FileUploadClient from '@/components/file-upload/FileUploadClient';

export const useBasicStore = defineStore('basic', {
  state: () => ({
    productName: 'Jarboot',
    version: '',
    uuid: '',
    host: '',
    workspace: 'workspace',
    clusterInitialized: false,
    inDocker: false,
    masterHost: '',
    os: '',
    jdk: '',
    dev: false,
    machineCode: '',
    innerHeight: window.innerHeight - 52,
    innerWidth: window.innerWidth,
    menus: [] as MenuItem[],
    subNameMap: new Map(),
    latestWeak: Date.now(),
    upgradeLoading: false,
    mobileDevice: CommonUtils.isMobileDevice(),
  }),
  actions: {
    async update() {
      const mobileDevice = CommonUtils.isMobileDevice();
      let innerHeight = window.innerHeight;
      if (mobileDevice) {
        innerHeight = innerHeight - 52;
      }
      this.$patch({ innerHeight, innerWidth: innerWidth, mobileDevice });
    },
    async init() {
      await this.update();
      const info = await Request.get<ServerRuntimeInfo>(`/api/jarboot/public/serverRuntime`, {});
      const productName = await Request.get<string>('/jarboot/preferences/productName', {});
      document.title = productName;
      const icon = document.head.querySelector('link[rel="icon"]');
      if (icon) {
        icon.setAttribute('href', `/jarboot/preferences/image/favicon.ico`);
      }
      this.$patch({ productName, ...info });
    },
    setMenus(menus: MenuItem[]) {
      this.$patch({ menus });
    },
    setVersion(version: string) {
      this.$patch({ version });
    },
  },
});

export const useUserStore = defineStore('user', {
  state: () => ({
    username: '',
    fullName: '',
    roles: '',
    userDir: '',
    avatar: null as string | null,
    privileges: null as any,
  }),

  actions: {
    async logout() {
      this.$patch({
        username: '',
        fullName: '',
        roles: '',
        userDir: '',
      });
      CommonUtils.deleteToken();
      await OAuthService.logout();
      return router.push({ name: PAGE_LOGIN });
    },
    async login(username: string, password: string) {
      const user: any = await OAuthService.login(username, password);
      this.$patch({ ...user });
      const name = router.currentRoute?.value?.query['redirect'] as string;
      if (name) {
        const paramsStr = router.currentRoute.value.query['redirectParams'] as string;
        let params = {};
        if (paramsStr) {
          params = JSON.parse(paramsStr);
        }
        const queryStr = router.currentRoute.value.query['redirectQuery'] as string;
        let query = {};
        if (queryStr) {
          query = JSON.parse(queryStr);
        }
        await router.push({ name, params, query });
        return;
      }
      await router.push('/');
    },
    setCurrentUser(user: any) {
      const privileges = { ...DEFAULT_PRIVILEGE, ...user.privileges } as any;
      this.$patch({ ...user, privileges });
    },
    async fetchPrivilege() {
      const privilegeList = (await PrivilegeService.getPrivilegeByRole(this.roles)) || [];
      const privileges = { ...DEFAULT_PRIVILEGE } as any;
      // 多角色权限合并
      privilegeList.forEach(privilege => (privileges[privilege.authCode] = privileges[privilege.authCode] || privilege.permission));
      this.$patch({ privileges });
      return privileges;
    },
    async fetchAvatar() {
      const avatar = await UserService.getAvatar(this.username);
      this.$patch({ avatar });
      return avatar;
    },
  },
});

export const useServiceStore = defineStore('services', {
  state: () => ({
    loading: true,
    search: '',
    groups: [] as ServiceInstance[],
    jvmGroups: [] as ServiceInstance[],
    jvmList: [] as JvmProcess[],
  }),
  actions: {
    async reload() {
      this.$patch({ loading: true });
      try {
        const result = (await ClusterManager.getServiceGroup()) as any;
        const groups = (result || []) as ServiceInstance[];
        this.$patch({ groups });
      } finally {
        this.$patch({ loading: false });
      }
    },
    async reloadJvmList() {
      this.$patch({ loading: true });
      try {
        const result = (await ClusterManager.getJvmProcesses()) as any;
        const jvmGroups = (result || []) as ServiceInstance[];
        this.$patch({ jvmGroups });
      } finally {
        this.$patch({ loading: false });
      }
    },

    attach(host: string, pid: string) {
      ClusterManager.attach(host, pid).then(r => console.log(r));
    },
    findInstance(groups: ServiceInstance[], sid: string): ServiceInstance | null {
      for (const g of groups) {
        if (g.sid === sid) {
          return g;
        }
        if (g.children?.length) {
          const s = this.findInstance(g.children, sid);
          if (s) {
            return s;
          }
        }
      }
      return null;
    },
    setStatus(sid: string, status: string, isService: boolean): ServiceInstance | null {
      const groups = isService ? this.groups : this.jvmGroups;
      const service = this.findInstance(groups, sid);

      if (service && service.status !== status) {
        const name = service.name;
        switch (status) {
          case STATUS_STARTING:
            // 激活终端显示
            service.status = status;
            Logger.log(`${name} 启动中...`);
            pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
            break;
          case STATUS_STOPPING:
            service.status = status;
            Logger.log(`${name} 停止中...`);
            pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
            break;
          case STATUS_STOPPED:
            service.status = status;
            Logger.log(`${name} 已停止`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case STATUS_SCHEDULING:
            service.status = status;
            Logger.log(`${name} 定时任务计划中`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case SCHEDULE_TASK_RUNNING:
            service.status = status;
            Logger.log(`${name} 定时任务已执行`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case STATUS_STARTED:
            if (!service.pid) {
              service.status = status;
            }
            Logger.log(`${name} 已启动`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            pubsub.publish(sid, PUB_TOPIC.FOCUS_CMD_INPUT);
            break;
          case ATTACHING:
            service.attaching = true;
            service.attached = false;
            Logger.log(`${name} ATTACHING`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case ATTACHED:
            service.attached = true;
            service.attaching = false;
            service.status = STATUS_ATTACHED;
            Logger.log(`${name} ATTACHED`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case EXITED:
            service.attached = false;
            service.attaching = false;
            service.status = STATUS_NOT_ATTACHED;
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            this.reloadJvmList().then(() => Logger.log(`${name} detached reload jvm.`));
            break;
          case TRUSTED:
            Logger.log(`${name} TRUSTED`);
            service.trusted = true;
            service.status = STATUS_ATTACHED;
            pubsub.publish(sid, CONSOLE_TOPIC.APPEND_LINE, CommonUtils.translate('TRUSTED_SUCCESS'));
            break;
          case NOT_TRUSTED:
            Logger.log(`${name} NOT_TRUSTED`);
            service.trusted = false;
            service.status = STATUS_ATTACHED;
            pubsub.publish(PUB_TOPIC.ROOT, PUB_TOPIC.NOT_TRUSTED, service);
            break;
          default:
            break;
        }
        if (isService) {
          this.$patch({ groups: [...groups] });
        } else {
          this.$patch({ jvmGroups: [...groups] });
        }
      } else {
        if (ATTACHED === status) {
          this.reloadJvmList().then(() => Logger.log(`${sid} attached reload jvm.`));
        }
      }
      return service;
    },
  },
});

export const useUploadStore = defineStore('upload-file', {
  state: () => ({
    uploadFiles: [] as UploadFileInfo[],
    clients: new Map<string, FileUploadClient>(),
    visible: false,
  }),
  actions: {
    update(file: UploadFileInfo) {
      let uploadFiles = [...this.uploadFiles];
      let visible = this.visible;
      const index = uploadFiles.findIndex(row => file.id === row.id);
      if (index < 0) {
        uploadFiles = [file, ...this.uploadFiles];
        visible = true;
      } else {
        uploadFiles[index] = file;
      }
      this.$patch({ uploadFiles, visible });
    },
    async upload(
      file: File,
      uploadMode: 'home' | 'service' | 'workspace' | '',
      baseDir: string,
      path: string,
      clusterHost: string,
      finishCallback?: (info: UploadFileInfo) => void
    ) {
      let client = new FileUploadClient(file, uploadMode, baseDir, path, clusterHost);
      if (finishCallback) {
        client.addFinishedEventHandler(finishCallback);
      }
      if (this.clients.has(client.getKey())) {
        client = this.clients.get(client.getKey()) as FileUploadClient;
        await client.upload();
        return;
      }
      client.addUploadEventHandler(info => this.update(info));
      client.addFinishedEventHandler(() => this.clients.delete(client.getKey()));
      this.clients.set(client.getKey(), client);
      await client.upload();
    },
    pause(dstPath: string) {
      const client = this.clients.get(dstPath);
      if (client) {
        client.pause();
      }
    },
    resume(dstPath: string) {
      const client = this.clients.get(dstPath);
      if (client) {
        client.upload();
      }
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot));
}
