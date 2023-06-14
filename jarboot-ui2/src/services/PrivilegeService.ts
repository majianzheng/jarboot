import Request from '@/common/Request';
import type { Privilege } from '@/types';

const urlBase = '/api/jarboot/privilege';

/**
 * 权限管理操作
 */
export default class PrivilegeService {
  public static savePrivilege(role: string, authCode: string, permission: boolean) {
    const form: FormData = new FormData();
    form.append('role', role);
    form.append('authCode', authCode);
    form.append('permission', '' + permission);
    return Request.put(urlBase, form);
  }

  public static hasPrivilege(role: string, username: string) {
    return Request.get<boolean>(urlBase, { role, username });
  }

  public static getPrivilegeByRole(role: string) {
    return Request.get<Privilege[]>(`${urlBase}/getPrivilegeByRole`, { role });
  }

  public static getPermissionInfos() {
    return Request.get(`${urlBase}/getPermissionInfos`, {});
  }
}
