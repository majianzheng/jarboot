import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from "@/services/OAuthService";
import CommonNotice from "@/common/CommonNotice";
import CommonUtils from "@/common/CommonUtils";
import router from "@/router";
import ServiceManager from "@/services/ServiceManager";
import type {ServiceInstance} from "@/common/CommonTypes";
import CommonConst from "@/common/CommonConst";
import Logger from "@/common/Logger";
import {PUB_TOPIC, pubsub} from "@/views/services/ServerPubsubImpl";
import {CONSOLE_TOPIC} from "@/common/CommonTypes";

export const useBasicStore = defineStore({
  id: 'basic',
  state: () => ({
    version: '',
    innerHeight: window.innerHeight,
    innerWidth: window.innerWidth,
  }),
  actions: {
    update() {
      this.$patch({innerHeight: window.innerHeight, innerWidth: window.innerWidth});
    },
    setVersion(version) {
      this.$patch({version});
    }
  }
});

export const useUserStore = defineStore({
  id: 'user',
  state: () => ({
    username: '',
    role: '',
  }),

  actions: {
    logout() {
      this.$patch({
        username: '',
        role: '',
      });
      CommonUtils.deleteToken();
      router.push('/login');
    },
    async login(username: string, password: string) {
      const resp = await OAuthService.login(username, password);
      if ((resp as any).resultCode !== 0) {
        CommonNotice.errorFormatted(resp);
        return;
      }
      const user: any = (resp as any).result;
      CommonUtils.storeToken(user.accessToken);
      this.$patch({...user});
      await router.push('/');
    },
    setCurrentUser(user: any) {
      this.$patch({...user});
    },
  },
});

export const useServicesStore = defineStore({
  id: 'services',
  state: () => ({
    loading: true,
    groups: [] as ServiceInstance[],
    instanceList: [] as ServiceInstance[],
    activatedList: [] as ServiceInstance[],
    activated: {} as ServiceInstance,
    // 当前选中的节点
    currentNode: [] as ServiceInstance[],
    checked: [] as ServiceInstance[],
  }),
  actions: {
    async reload() {
      this.$patch({loading: true});
      const resp = (await ServiceManager.getServiceGroup()) as any;
      if (0 !== resp.resultCode) {
        return;
      }
      const groups = resp.result || [];
      const instanceList = [];
      groups.forEach(group => ((group.children||[]).forEach(s => instanceList.push(s))));
      this.$patch({groups, instanceList, loading: false});
    },
    startServices() {
      const services = [];
      this.currentNode.forEach(node => {
            if (node.children?.length > 0) {
              services.push(...node.children);
            } else {
              services.push(node);
            }
      });
      ServiceManager.startService(services, () => {});
    },
    stopServices() {
      const services = [];
      this.currentNode.forEach(node => {
        if (node.children?.length > 0) {
          services.push(...node.children);
        } else {
          services.push(node);
        }
      });
      ServiceManager.stopService(services, () => {});
    },
    currentChange(data, node) {
      if (node.isLeaf) {
        const index = this.activatedList.findIndex(item => item.sid === data.sid);
        if (-1 === index) {
          const activatedList = [...this.activatedList, data];
          this.$patch({activated: data, activatedList});
        } else {
          this.$patch({activated: data});
        }
      }
      this.$patch({currentNode: [data]});
    },
    closeServiceTerminal(instance) {
      const activatedList = this.activatedList.filter(item => item.sid !== instance.sid);
      let activated = {...this.activated};
      if (this.activated.sid === instance.sid) {
        if (activatedList.length > 0) {
          activated = activatedList[0];
        } else {
          activated = {};
        }
      }
      this.$patch({activated, activatedList});
    },
    checkChange(checked) {
      this.$patch({checked});
    },
    setStatus(sid: string, status: string) {
      const groups = [...this.groups];
      for (let i = 0; i < groups.length; ++i) {
        const group = groups[i];
        const children = group.children || [];
        const service = children.find(item => (item.sid === sid));
        if (service && service.status !== status) {
          if (CommonConst.STATUS_STARTING === status) {
            this.$patch({activated: service});
          }
          service.status = status;
          this.$patch({groups});
          const name = service.name;
          switch (status) {
            case CommonConst.STATUS_STARTING:
              // 激活终端显示
              //activeConsole(key);
              Logger.log(`${name} 启动中...`);
              pubsub.publish(sid, CONSOLE_TOPIC.CLEAR_CONSOLE);
              pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
              break;
            case CommonConst.STATUS_STOPPING:
              Logger.log(`${name} 停止中...`);
              pubsub.publish(sid, CONSOLE_TOPIC.START_LOADING);
              break;
            case CommonConst.STATUS_STARTED:
              Logger.log(`${name} 已启动`);
              pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
              pubsub.publish(sid, PUB_TOPIC.FOCUS_CMD_INPUT);
              break;
            case CommonConst.STATUS_STOPPED:
              Logger.log(`${name} 已停止`);
              pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
              break;
            default:
              return {};
          }
          return;
        }
      }
    }
  }
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot))
}
