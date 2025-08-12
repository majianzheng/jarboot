import axios from 'axios';
import CommonUtils from './CommonUtils';
import router from '../router';
import CommonNotice from '@/common/CommonNotice';
import Logger from '@/common/Logger';
import type { AxiosProgressEvent } from 'axios';
import { PAGE_LOGIN } from '@/common/route-name-constants';

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
   * @param file 请求参数
   * @param onUploadProgress
   * @param params
   */
  static upload<T>(url: string, file: File, onUploadProgress?: (progressEvent: AxiosProgressEvent) => void, params?: Map<string, string>) {
    const form: FormData = new FormData();
    if (file) {
      form.append('file', file);
    } else {
      Logger.error('file is null.', file);
      return Promise.reject(new Error('file is null'));
    }
    if (params) {
      params.forEach((value, key) => form.append(key, value));
    }
    return http.post<any, T>(url, form, { onUploadProgress });
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

  private static async gotoLogin() {
    CommonUtils.deleteToken();
    const query = CommonUtils.parseRedirectQuery(router.currentRoute.value);
    return router.push({ name: PAGE_LOGIN, query }).then(() => console.info('未登陆，跳转到登陆界面...'));
  }

  public static init() {
    http.interceptors.response.use(
      response => {
        if (401 === response?.status) {
          //没有授权，跳转到登录界面
          return Request.gotoLogin();
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
          return Request.gotoLogin();
        }
        if (resultCode != 0) {
          CommonNotice.error(data.msg || '请求服务器失败');
          return Promise.reject(data);
        }
        const result = data?.data;
        return Promise.resolve(result ?? data);
      },
      error => {
        if (error.config && error.config.loading) {
          error.config.loading.close();
        }
        const msg = `请求发生错误：${error.message || ''}`;
        console.error(msg, error);
        if (401 === error.response?.status) {
          //没有授权，跳转到登录界面
          return Request.gotoLogin();
        }
        CommonNotice.error('请求服务器失败');
        return Promise.reject(error);
      }
    );

    // 请求拦截器，塞入token以便鉴权
    http.interceptors.request.use(request => {
      return request;
    });
  }
}

Request.init();
