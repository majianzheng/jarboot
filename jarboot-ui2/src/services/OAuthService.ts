import Request from '@/common/Request';

const urlBase = '/api/jarboot/auth';

/**
 * 鉴权
 */
export default class OAuthService {
  /**
   * 获取当前用户
   */
  public static getCurrentUser() {
    return Request.get(`${urlBase}/getCurrentUser`, {});
  }

  /**
   * 登录
   * @param username
   * @param password
   */
  public static login(username?: string, password?: string) {
    const form: FormData = new FormData();
    if (username && username.length > 0) {
      form.append('username', username);
    }
    if (password && password.length > 0) {
      form.append('password', password);
    }
    return Request.post<any>(`${urlBase}/login`, form);
  }
}
