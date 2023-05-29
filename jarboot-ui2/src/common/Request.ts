import axios from 'axios';
import CommonUtils from './CommonUtils';
import router from '../router';
import CommonNotice from '@/common/CommonNotice';

const http = axios.create({
  baseURL: '',
  timeout: 30000,
});

/**
 * Http请求封装
 * @author majianzheng
 */
export default class Request {
  /**
   *
   * @param url
   * @param params 请求参数Map
   */
  static get<T>(url: string, params: any) {
    return http.get<any, T>(url, { params: params });
  }

  /**
   *
   * @param url 请求地址
   * @param params 请求参数
   */
  static post<T>(url: string, params: any) {
    return http.post<any, T>(url, params);
  }

  /**
   *
   * @param url 请求地址
   * @param params 请求参数
   */
  static put<T>(url: string, params: any) {
    return http.put<any, T>(url, params);
  }

  /**
   *
   * @param url 请求地址
   * @param params 请求参数
   */
  static delete<T>(url: string, params: any) {
    return http.delete<any, T>(url, params);
  }

  public static init() {
    http.interceptors.response.use(
      response => {
        if (401 === response?.status) {
          //没有授权，跳转到登录界面
          CommonUtils.deleteToken();
          return router.push({ path: '/login' }).then(() => console.info('未登陆，跳转到登陆界面...'));
        }
        let data = response.data;
        if (typeof data == 'string' && (data.startsWith('{') || data.startsWith('['))) {
          try {
            const temp = JSON.parse(data);
            if (temp) {
              data = temp;
            }
          } catch (error) {
            //
          }
        }
        if (typeof data == 'string') {
          return Promise.resolve(data);
        }
        const resultCode = data.code ?? 0;
        if (resultCode === 401) {
          CommonNotice.warn(data.msg || '请登录！');
          CommonUtils.deleteToken();
          return router.push({ name: 'login' });
        }
        if (resultCode != 0) {
          CommonNotice.error(data.msg || '请求服务器失败');
          return Promise.reject(data);
        }
        const result = data?.data;
        return Promise.resolve(result || data);
      },
      error => {
        if (error.config && error.config.loading) {
          error.config.loading.close();
        }
        const msg = `请求发生错误：${error.message || ''}`;
        console.error(msg, error);
        CommonNotice.error('请求服务器失败');
        return Promise.reject(error);
      }
    );

    // 请求拦截器，塞入token以便鉴权
    http.interceptors.request.use(request => {
      const token = CommonUtils.getToken();
      if (request.headers && token.length) {
        request.headers['Authorization'] = token;
      }
      return request;
    });
  }
}

Request.init();
