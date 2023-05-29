import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from '@/services/OAuthService';
import CommonUtils from '@/common/CommonUtils';
import router from '@/router';
import ServiceManager from '@/services/ServiceManager';
import type { ServiceInstance, JvmProcess } from '@/types';
import { ATTACHED, ATTACHING, EXITED, STATUS_STARTED, STATUS_STARTING, STATUS_STOPPED, STATUS_STOPPING } from '@/common/CommonConst';
import Logger from '@/common/Logger';
import { PUB_TOPIC, pubsub } from '@/views/services/ServerPubsubImpl';
import { CONSOLE_TOPIC } from '@/types';

export const useBasicStore = defineStore({
  id: 'basic',
  state: () => ({
    version: '',
    innerHeight: window.innerHeight,
    innerWidth: window.innerWidth,
  }),
  actions: {
    update() {
      this.$patch({ innerHeight: window.innerHeight, innerWidth: window.innerWidth });
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
      const user: any = await OAuthService.login(username, password);
      CommonUtils.storeToken(user.accessToken);
      this.$patch({ ...user });
      await router.push('/');
    },
    setCurrentUser(user: any) {
      this.$patch({ ...user });
    },
  },
});

export const useServicesStore = defineStore({
  id: 'services',
  state: () => ({
    loading: true,
    search: '',
    currentTab: 'service',
    groups: [] as ServiceInstance[],
    instanceList: [] as ServiceInstance[],
    jvmGroups: [] as JvmProcess[],
    jvmList: [] as JvmProcess[],
    activatedList: [] as ServiceInstance[],
    activated: {} as ServiceInstance,
    // 当前选中的节点
    currentNode: [] as ServiceInstance[],
    checked: [] as ServiceInstance[],
  }),
  actions: {
    async reload() {
      this.$patch({ loading: true });
      const result = (await ServiceManager.getServiceGroup()) as any;
      const groups = (result || []) as ServiceInstance[];
      const instanceList = [] as ServiceInstance[];
      groups.forEach(group =>
        (group.children || []).forEach(s => {
          if (s.sid) {
            instanceList.push(s);
          } else {
            (s.children || []).forEach(c => {
              if (c.sid) {
                instanceList.push(c);
              } else {
                (c.children || []).forEach(c1 => {
                  if (c1.sid) {
                    instanceList.push(c1);
                  }
                });
              }
            });
          }
        })
      );
      this.$patch({ groups, instanceList, loading: false });
    },
    async reloadJvmList() {
      this.$patch({ loading: true });
      const result = (await ServiceManager.getJvmProcesses()) as any;
      const jvmGroups = (result || []) as JvmProcess[];
      const jvmList = [] as JvmProcess[];
      jvmGroups.forEach(group => (group.children || []).forEach(s => jvmList.push(s)));
      this.$patch({ jvmGroups, jvmList, loading: false });
    },

    attach(pid: number) {
      ServiceManager.attach(pid).then(r => console.log(r));
    },
    getSelected() {
      const services = [] as ServiceInstance[];
      this.currentNode.forEach((node: ServiceInstance) => {
        if (node?.children && node.children.length > 0) {
          services.push(...node.children);
        } else {
          services.push(node);
        }
      });
      return services;
    },
    startServices() {
      ServiceManager.startService(this.getSelected(), () => {});
    },
    stopServices() {
      ServiceManager.stopService(this.getSelected(), () => {});
    },
    currentChange(data: ServiceInstance, node: any) {
      if (node.isLeaf) {
        const index = this.activatedList.findIndex(item => item.sid === data.sid);
        if (-1 === index) {
          const activatedList = [...this.activatedList, data];
          this.$patch({ activated: data, activatedList });
        } else {
          this.$patch({ activated: data });
        }
      }
      this.$patch({ currentNode: [data] });
    },
    closeServiceTerminal(instance: ServiceInstance) {
      const activatedList = this.activatedList.filter(item => item.sid !== instance.sid);
      let activated = { ...this.activated };
      if (this.activated.sid === instance.sid) {
        if (activatedList.length > 0) {
          activated = activatedList[0];
        } else {
          activated = {} as ServiceInstance;
        }
      }
      this.$patch({ activated, activatedList });
    },
    checkChange(checked: ServiceInstance[]) {
      this.$patch({ checked });
    },
    setStatus(sid: string, status: string) {
      const groups = [...this.groups];
      const service = this.instanceList.find(item => item.sid === sid);

      if (service && service.status !== status) {
        if (STATUS_STARTING === status) {
          this.$patch({ activated: service });
        }
        console.info('status change,', status, service);
        const name = service.name;
        switch (status) {
          case STATUS_STARTING:
            // 激活终端显示
            //activeConsole(key);
            service.status = status;
            Logger.log(`${name} 启动中...`);
            pubsub.publish(sid, CONSOLE_TOPIC.CLEAR_CONSOLE);
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
            Logger.log(`${name} 已停止`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case ATTACHED:
            service.attached = true;
            service.attaching = false;
            Logger.log(`${name} 已停止`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          case EXITED:
            service.attached = false;
            service.attaching = false;
            Logger.log(`${name} 已停止`);
            pubsub.publish(sid, CONSOLE_TOPIC.FINISH_LOADING);
            break;
          default:
            return {};
        }
        this.$patch({ groups });
      }
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot));
}
