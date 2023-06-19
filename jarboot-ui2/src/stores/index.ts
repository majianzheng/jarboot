import { defineStore, acceptHMRUpdate } from 'pinia';
import OAuthService from '@/services/OAuthService';
import CommonUtils from '@/common/CommonUtils';
import router from '@/router';
import ServiceManager from '@/services/ServiceManager';
import type { ServiceInstance, JvmProcess } from '@/types';
import { PAGE_LOGIN } from '@/common/route-name-constants';
import PrivilegeService from '@/services/PrivilegeService';
import { DEFAULT_PRIVILEGE } from '@/common/CommonConst';
import UserService from '@/services/UserService';

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
      router.push({ name: PAGE_LOGIN }).then(r => {});
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
