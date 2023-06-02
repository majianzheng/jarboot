import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from '@/services/OAuthService';
import CommonUtils from '@/common/CommonUtils';
import router from '@/router';
import ServiceManager from '@/services/ServiceManager';
import type { ServiceInstance, JvmProcess } from '@/types';

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
    groups: [] as ServiceInstance[],
    jvmGroups: [] as ServiceInstance[],
    jvmList: [] as JvmProcess[],
  }),
  actions: {
    async reload() {
      this.$patch({ loading: true });
      const result = (await ServiceManager.getServiceGroup()) as any;
      const groups = (result || []) as ServiceInstance[];
      this.$patch({ groups, loading: false });
    },
    async reloadJvmList() {
      this.$patch({ loading: true });
      const result = (await ServiceManager.getJvmProcesses()) as any;
      const jvmGroups = (result || []) as ServiceInstance[];
      this.$patch({ jvmGroups, loading: false });
    },

    attach(pid: number) {
      ServiceManager.attach(pid).then(r => console.log(r));
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot));
}
