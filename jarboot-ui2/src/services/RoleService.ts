import Request from '@/common/Request';
import type { ResponseVo, RoleInfo } from '@/types';

const urlBase = '/api/jarboot/role';

/**
 * 用户管理操作
 */
export default class RoleService {
  public static addRole(role: string, name: string) {
    const form: FormData = new FormData();
    form.append('role', role);
    form.append('name', name);
    return Request.put<ResponseVo>(urlBase, form);
  }

  public static setRoleName(role: string, name: string) {
    const form: FormData = new FormData();
    form.append('role', role);
    form.append('name', name);
    return Request.put<ResponseVo>(urlBase + '/name', form);
  }

  public static deleteRole(role: string) {
    return Request.delete<ResponseVo>(`${urlBase}?role=${role}`, {});
  }

  public static getRoles(role: string, name: string, pageNo: number, pageSize: number) {
    return Request.get(`${urlBase}/getRoles`, { role, name, pageNo, pageSize });
  }

  public static getRoleList() {
    return Request.get<RoleInfo[]>(`${urlBase}/getRoleList`, {});
  }
}
