import Request from '@/common/Request';
import type { ServiceInstance, ServerSetting } from '@/types';
import Logger from '@/common/Logger';

const urlBase = '/api/jarboot/cluster/manager';
/**
 * 服务管理
 */
export default class ClusterManager {
  /**
   * 获取存活的集群实例
   */
  public static getOnlineClusterHosts() {
    return Request.get<{ host: string; name: string }[]>(urlBase + '/onlineClusterHosts', {});
  }

  /**
   * 获取服务组
   */
  public static getServiceGroup() {
    return Request.get(urlBase + '/services', {});
  }

  /**
   * 启动服务
   * @param services
   */
  public static startService(services: ServiceInstance[]) {
    const param = ClusterManager.parseParam(services);
    return Request.post(`${urlBase}/startServices`, param);
  }

  /**
   * 终止服务
   * @param services
   */
  public static stopService(services: ServiceInstance[]) {
    const param = ClusterManager.parseParam(services);
    return Request.post(`${urlBase}/stopServices`, param);
  }

  /**
   * 重启服务
   * @param services
   */
  public static restartService(services: ServiceInstance[]) {
    const param = ClusterManager.parseParam(services);
    return Request.post(`${urlBase}/restartServices`, param);
  }

  /**
   * 获取未被服务管理的JVM进程信息
   */
  public static getJvmProcesses() {
    return Request.get(`${urlBase}/jvmGroups`, {});
  }

  /**
   * attach进程
   * @param host host
   * @param pid pid
   */
  public static attach(host: string, pid: string) {
    const form = new FormData();
    form.append('host', host);
    form.append('pid', pid);
    return Request.post(`${urlBase}/attach`, form);
  }

  /**
   * 删除服务
   * @param instance 服务
   */
  public static deleteService(instance: ServiceInstance[]) {
    return Request.post(`${urlBase}/deleteService`, instance);
  }

  /**
   * 获取服务配置
   * @param instance 服务
   * @returns {Promise<ServerSetting>}
   */
  public static getServerSetting(instance: ServiceInstance): Promise<ServerSetting> {
    return Request.post<ServerSetting>(`${urlBase}/serviceSetting`, instance);
  }

  /**
   * 保存服务配置
   * @param setting 服务
   * @returns {Promise<string>}
   */
  public static saveServerSetting(setting: ServerSetting): Promise<string> {
    return Request.post<string>(`${urlBase}/saveServiceSetting`, setting);
  }

  public static importService(file: File, clusterHost?: string) {
    const form: Map<string, string> = new Map<string, string>();
    if (!file) {
      Logger.error('file is null.', file);
      return Promise.reject(new Error('file is null'));
    }
    if (clusterHost) {
      form.set('clusterHost', clusterHost);
    }
    return Request.upload(`${urlBase}/importService`, file, undefined, form);
  }

  private static parseParam(services: ServiceInstance[]): ServiceInstance[] {
    const instances = [] as ServiceInstance[];
    const sets = new Set<string>();
    services.forEach(value => {
      if (!value.children?.length && value.name) {
        const key = `${value.host}-${value.name}`;
        if (!sets.has(key)) {
          instances.push(value);
          sets.add(key);
        }
        return;
      }
      const children = value.children as ServiceInstance[];
      children?.length &&
        children.forEach(child => {
          const key = `${child.host}-${child.name}`;
          if (!sets.has(key)) {
            instances.push(child);
            sets.add(key);
          }
        });
    });
    return instances;
  }
}
