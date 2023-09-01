import Request from '../common/Request';
import type { GlobalSetting, ResponseVo, ServerSetting } from '@/types';

const settingUrl = '/api/jarboot/setting';

/**
 * 配置服务
 */
export default class SettingService {
  /**
   * 获取服务配置
   * @param serviceName 服务名
   * @returns {Promise<ServerSetting>}
   */
  public static getServerSetting(serviceName: string): Promise<ServerSetting> {
    return Request.get<ServerSetting>(`${settingUrl}/serviceSetting`, { serviceName });
  }

  /**
   * 提交服务配置
   * @param setting 配置信息
   */
  public static submitServerSetting(setting: ServerSetting) {
    return Request.post<ResponseVo>(`${settingUrl}/serviceSetting`, setting);
  }

  /**
   * 获取服务配置
   * @returns {Promise<GlobalSetting>}
   */
  public static getGlobalSetting(): Promise<GlobalSetting> {
    return Request.get<GlobalSetting>(`${settingUrl}/globalSetting`, {});
  }

  /**
   * 提交服务配置
   * @param setting 配置信息
   */
  public static submitGlobalSetting(setting: GlobalSetting) {
    return Request.post<ResponseVo>(`${settingUrl}/globalSetting`, setting);
  }

  /**
   * 添加信任的远程主机
   * @param host 地址
   */
  public static addTrustedHost(host: string) {
    const form = new FormData();
    form.append('host', host);
    return Request.post<ResponseVo>(`${settingUrl}/trustedHost`, form);
  }

  /**
   * 添加信任的远程主机
   * @param host 地址
   */
  public static removeTrustedHost(host: string) {
    const form = new FormData();
    form.append('host', host);
    return Request.delete<ResponseVo>(`${settingUrl}/trustedHost?host=${host}`, form);
  }

  /**
   * 添加信任的远程主机
   */
  public static getTrustedHosts() {
    return Request.get<string[]>(`${settingUrl}/trustedHost`, {});
  }
}
