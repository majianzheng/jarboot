import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from '@/services/OAuthService';
import CommonUtils from '@/common/CommonUtils';
import router from '@/router';
import ClusterManager from '@/services/ClusterManager';
import type { ServiceInstance, JvmProcess, ServerRuntimeInfo, UploadFileInfo } from '@/types';
import { PAGE_LOGIN } from '@/common/route-name-constants';
import PrivilegeService from '@/services/PrivilegeService';
import {
  ATTACHED,
  ATTACHING,
  DEFAULT_PRIVILEGE,
  EXITED,
  NOT_TRUSTED,
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

export const useBasicStore = defineStore({
  id: 'basic',
  state: () => ({
    version: '',
    uuid: '',
    host: '',
    inDocker: false,
    masterHost: '',
    innerHeight: window.innerHeight,
    innerWidth: window.innerWidth,
  }),
  actions: {
    async update() {
      this.$patch({ innerHeight: window.innerHeight, innerWidth: window.innerWidth });
    },
    async init() {
      const info = await Request.get<ServerRuntimeInfo>(`/api/jarboot/public/serverRuntime`, {});
      this.$patch({ ...info });
    },
    setVersion(version: string) {
      this.$patch({ version });
    },
  },
});

export const useUserStore = defineStore({
  id: 'user',
  state: () => ({
    username: '',
    fullName: '',
    roles: '',
    userDir: '',
    avatar: null as string | null,
    permission: null as string | null,
  }),

  actions: {
    logout() {
      this.$patch({
        username: '',
        fullName: '',
        roles: '',
        userDir: '',
      });
      CommonUtils.deleteToken();
      return router.push({ name: PAGE_LOGIN });
    },
    async login(username: string, password: string) {
      const user: any = await OAuthService.login(username, password);
      CommonUtils.storeToken(user.accessToken);
      CommonUtils.storeCurrentHost(user.host);
      this.$patch({ ...user });
      await router.push('/');
    },
    setCurrentUser(user: any) {
      this.$patch({ ...user });
    },
    async fetchPrivilege() {
      const privilegeList = (await PrivilegeService.getPrivilegeByRole(this.roles)) || [];
      const permission = { ...DEFAULT_PRIVILEGE } as any;
      // 多角色权限合并
      privilegeList.forEach(privilege => (permission[privilege.authCode] = permission[privilege.authCode] || privilege.permission));
      this.$patch({ permission });
      return permission;
    },
    async fetchAvatar() {
      const avatar = await UserService.getAvatar(this.username);
      this.$patch({ avatar });
      return avatar;
    },
  },
});

export const useServiceStore = defineStore({
  id: 'services',
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

export const useUploadStore = defineStore({
  id: 'upload-file',
  state: () => ({
    uploadFiles: [] as UploadFileInfo[],
    visible: false,
  }),
  actions: {
    async update(file: UploadFileInfo) {
      const uploadFiles = [...this.uploadFiles];
      let visible = this.visible;
      const index = uploadFiles.findIndex(row => file.id === row.id);
      if (index < 0) {
        uploadFiles.push(file);
        visible = true;
      } else {
        uploadFiles[index] = file;
      }
      this.$patch({ uploadFiles, visible });
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot));
}
