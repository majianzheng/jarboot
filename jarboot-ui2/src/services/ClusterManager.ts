import Request from '@/common/Request';
import type { ServiceInstance, ServerSetting } from '@/types';

const urlBase = '/api/jarboot/cluster/manager';
/**
 * 服务管理
 */
export default class ClusterManager {
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
   * @param callback
   */
  public static stopService(services: ServiceInstance[]) {
    const param = ClusterManager.parseParam(services);
    return Request.post(`${urlBase}/stopServices`, param);
  }

  /**
   * 重启服务
   * @param services
   * @param callback
   */
  public static restartService(services: ServiceInstance[], callback: any) {
    const param = ClusterManager.parseParam(services);
    Request.post(`${urlBase}/restartServices`, param).then(callback);
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
  public static attach(host: string, pid: number) {
    return Request.post(`${urlBase}/attach`, { host, pid });
  }

  /**
   * 删除服务
   * @param instance 服务
   */
  public static deleteService(instance: ServiceInstance) {
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
