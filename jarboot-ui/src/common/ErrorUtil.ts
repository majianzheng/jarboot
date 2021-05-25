import StringUtil from './StringUtil';

/**
 * http错误处理公用类
 */
export default class ErrorUtil {
  /**
   * 翻译 1.服务器返回的response带的错误信息 2.ajax调用error中返回的msg,形式:{status:,statusText:}
   * 目前翻译方法如下:
   *   形式: 错误信息 (错误码),例如 出错(0X0001)
   *   错误码,转换为十六进制表示,若为负数,直接转为正数表示
   * @param resp
   */
  public static formatErrResp(resp: any) {
    let resultCode: number|string = 0;
    let resultMsg = '';
    if (StringUtil.isNotNull(resp.resultCode)) {
      //服务器返回的resp
      resultCode = resp.resultCode;
      if (resultCode >= 0) {
        resultCode = '0x' + resultCode.toString(16).toUpperCase();
      } else {
          if (typeof resultCode === "string") {
              resultCode = '0x' + (parseInt(resultCode, 10) >>> 0).toString(16).toUpperCase();
          }
      }
      resultMsg = resp.resultMsg;
    } else if (StringUtil.isNotNull(resp.status)) {
      //ajax error中的msg
      resultCode = resp.status;
      resultMsg = resp.statusText;
      if (resp.status === 401) {
        resultMsg = '登录失效,请尝试刷新页面、重新登录';
      } else if (resp.status === 403) {
        resultMsg = '权限验证失败,请检查权限';
      }
    } else {
      //http 代码 500等
      resultCode = resp;
    }
    return `${resultMsg} (错误码:${resultCode})`;
  }
}
