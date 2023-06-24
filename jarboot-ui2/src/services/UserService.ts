import Request from '@/common/Request';
import type { ResponseVo } from '@/types';

const urlBase = '/api/jarboot/user';

/**
 * 用户管理操作
 */
export default class UserService {
  public static createUser(username: string, fullName: string, password: string, roles: string, userDir: string, avatar: string | null = null) {
    const form: FormData = new FormData();
    form.append('username', username);
    form.append('fullName', fullName);
    form.append('password', password);
    form.append('roles', roles);
    form.append('userDir', userDir || '');
    avatar && form.append('avatar', avatar);
    return Request.post<ResponseVo>(urlBase, form);
  }

  public static updateUser(
    username: string,
    fullName: string | null,
    roles: string | null,
    userDir: string | null,
    avatar: string | null = null
  ) {
    const form: FormData = new FormData();
    form.append('username', username);
    form.append('fullName', fullName || '');
    form.append('roles', roles || '');
    form.append('userDir', userDir || '');
    avatar && form.append('avatar', avatar);
    return Request.post<ResponseVo>(urlBase + '/update', form);
  }

  public static deleteUser(id: number) {
    const form: FormData = new FormData();
    form.append('id', '' + id);
    return Request.delete<ResponseVo>(urlBase, form);
  }

  public static updateUserPassword(username: string, password: string, oldPassword: string) {
    const form: FormData = new FormData();
    form.append('username', username);
    form.append('password', password);
    form.append('oldPassword', oldPassword || '');
    return Request.put<ResponseVo>(urlBase, form);
  }

  public static getUsers(username: string, role: string, pageNo: number, pageSize: number) {
    return Request.get(`${urlBase}/getUsers`, { username, role, pageNo, pageSize });
  }

  public static getAvatar(username: string) {
    return Request.get<string>(`${urlBase}/avatar`, { username });
  }

  public static getUserDirs() {
    return Request.get<string[]>(`${urlBase}/userDirs`, {});
  }
}
