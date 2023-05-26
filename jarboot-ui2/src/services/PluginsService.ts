import Request from '../common/Request';

const baseUrl = '/api/jarboot/plugins';

/**
 * 文件上传
 */
export default class PluginsService {
  /**
   * 获取插件列表
   */
  public static getAgentPlugins() {
    return Request.get(baseUrl, {});
  }

  /**
   * 移除插件
   * @param type
   * @param filename
   */
  public static removePlugin(type: string, filename: string) {
    const form = new FormData();
    form.append('type', type);
    form.append('filename', filename);
    return Request.delete(baseUrl, form);
  }
}
