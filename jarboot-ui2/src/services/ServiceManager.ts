import Request from '@/common/Request';
import StringUtil from '@/common/StringUtil';
import type { ServiceInstance } from '@/types';

const urlBase = '/api/jarboot/services';
/**
 * 服务管理
 */
export default class ServiceManager {
  /**
   * 获取服务组
   */
  public static getServiceGroup() {
    return Request.get(urlBase + '/groups', {});
  }

  /**
   * 启动服务
   * @param services
   */
  public static startService(services: ServiceInstance[]) {
    const param = ServiceManager.parseParam(services);
    return Request.post(`${urlBase}/startService`, param);
  }

  /**
   * 终止服务
   * @param services
   * @param callback
   */
  public static stopService(services: ServiceInstance[]) {
    const param = ServiceManager.parseParam(services);
    return Request.post(`${urlBase}/stopService`, param);
  }

  /**
   * 重启服务
   * @param services
   * @param callback
   */
  public static restartService(services: ServiceInstance[], callback: any) {
    const param = ServiceManager.parseParam(services);
    Request.post(`${urlBase}/restartService`, param).then(callback);
  }

  /**
   * 一键重启
   */
  public static oneClickRestart() {
    return Request.get(`${urlBase}/oneClickRestart`, {});
  }

  /**
   * 一键启动
   */
  public static oneClickStart() {
    return Request.get(`${urlBase}/oneClickStart`, {});
  }

  /**
   * 一键停止
   */
  public static oneClickStop() {
    return Request.get(`${urlBase}/oneClickStop`, {});
  }

  /**
   * base64编码
   * @param data
   */
  public static base64Encoder(data: string) {
    if (StringUtil.isEmpty(data)) {
      return Promise.resolve();
    }
    return Request.get(`${urlBase}/base64Encoder`, { data });
  }

  /**
   * 获取未被服务管理的JVM进程信息
   */
  public static getJvmProcesses() {
    return Request.get(`${urlBase}/jvmGroups`, {});
  }

  /**
   * attach进程
   * @param pid pid
   */
  public static attach(pid: number) {
    return Request.get(`${urlBase}/attach`, { pid });
  }

  /**
   * 删除服务
   * @param serviceName 服务名
   */
  public static deleteService(serviceName: string) {
    const form = new FormData();
    form.append('serviceName', serviceName);
    return Request.delete(`${urlBase}/service`, form);
  }

  private static parseParam(services: ServiceInstance[]): string[] {
    const set = new Set<string>();
    services.forEach(value => {
      if (!value.children?.length && value.name) {
        set.add(value.name);
        return;
      }
      const children = value.children as ServiceInstance[];
      children?.length && children.forEach(child => set.add(child.name));
    });
    return Array.from(set);
  }
}
