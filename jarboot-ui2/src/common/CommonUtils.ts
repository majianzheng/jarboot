import { ACCESS_CLUSTER_HOST, TOKEN_KEY } from './CommonConst';
import { getCurrentInstance } from 'vue';
import type { I18n, Locale } from 'vue-i18n';

/**
 * @author majianzheng
 */
export default class CommonUtils {
  private static readonly HOME_PREFIX = '/jarboot/';
  private static readonly TOKEN_PREFIX = 'Bearer ';
  public static readonly ACCESS_TOKEN = 'accessToken';
  private static t: any;
  private static i18n: I18n;
  public static init(i18n: I18n) {
    CommonUtils.i18n = i18n;
    CommonUtils.t = getCurrentInstance()?.appContext.config.globalProperties.$t;
  }

  public static translate(s: string, ...args: any[]) {
    let msg = (CommonUtils.t ? CommonUtils.t(s) : s) as string;
    //{size}
    if (!args?.length) {
      return msg;
    }
    args.forEach((arg: any) => {
      for (const key in arg) {
        const reg = `{${key}}`;
        msg = msg.replaceAll(reg, arg[key]);
      }
    });
    return msg;
  }

  public static mergeLocaleMessage(locale: Locale, message: any) {
    CommonUtils.i18n.global.mergeLocaleMessage(locale, message);
  }

  public static getToken(): string {
    let token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      token = '';
    }
    return token;
  }

  public static storeToken(token: string) {
    if (0 !== token.indexOf(CommonUtils.TOKEN_PREFIX)) {
      token = CommonUtils.TOKEN_PREFIX + token;
    }
    localStorage.setItem(TOKEN_KEY, token);
  }

  public static storeCurrentHost(host: string) {
    if (host) {
      localStorage.setItem(ACCESS_CLUSTER_HOST, host);
    }
  }

  public static getCurrentHost() {
    return localStorage.getItem(ACCESS_CLUSTER_HOST) || '';
  }

  public static deleteToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  public static getRawToken(): string {
    let token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      return '';
    }
    if (0 === token.indexOf(CommonUtils.TOKEN_PREFIX)) {
      token = token.substring(CommonUtils.TOKEN_PREFIX.length);
    }
    return token;
  }

  public static exportServer(name: string, clusterHost: string): void {
    const a = document.createElement('a');
    const token = CommonUtils.getRawToken();
    const host = CommonUtils.getCurrentHost();
    let url = `/api/jarboot/cluster/manager/exportService?name=${name}&${CommonUtils.ACCESS_TOKEN}=${token}&clusterHost=${clusterHost}`;
    if (host) {
      url += `&${ACCESS_CLUSTER_HOST}=${host}`;
    }
    a.href = url;
    a.click();
    a.remove();
  }

  public static download(url: string, filename: string, method = 'GET', body: any = '', callback?: (result: boolean, msg?: string) => void) {
    const xhr = new XMLHttpRequest();
    //GET请求,请求路径url,async(是否异步)
    xhr.open(method, url, true);
    //设置请求头参数
    xhr.setRequestHeader('Authorization', CommonUtils.getToken());
    const host = CommonUtils.getCurrentHost();
    if (host) {
      xhr.setRequestHeader(ACCESS_CLUSTER_HOST, host);
    }
    //设置响应类型为 blob
    xhr.responseType = 'blob';
    //关键部分
    xhr.onload = function () {
      //如果请求执行成功
      if (this.status == 200) {
        const blob = this.response;
        const a = document.createElement('a');
        //创键临时url对象
        const objUrl = URL.createObjectURL(blob);
        a.href = objUrl;
        a.download = filename;
        a.click();
        //释放之前创建的URL对象
        window.URL.revokeObjectURL(objUrl);
        a.remove();
        callback && callback(true);
      } else {
        callback && callback(false, '下载失败，状态码：' + this.status);
      }
    };
    //发送请求
    xhr.send(body);
  }

  public static downloadTextAsFile(text: string, filename: string) {
    const a = document.createElement('a');
    //创键临时url对象
    const objUrl = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
    a.href = objUrl;
    a.download = filename;
    a.click();
    //释放之前创建的URL对象
    window.URL.revokeObjectURL(objUrl);
    a.remove();
  }
}
