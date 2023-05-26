import Request from '@/common/Request';
import CommonNotice from '@/common/CommonNotice';
import CommonUtils from '@/common/CommonUtils';
import StringUtil from '@/common/StringUtil';
import type { ServiceInstance } from '@/common/CommonTypes';

const urlBase = '/api/jarboot/services';
/**
 * 服务管理
 */
export default class ServiceManager {
  /**
   * 获取服务列表
   * @param callback
   */
  public static getServiceList(callback: any) {
    Request.get(urlBase, {}).then(callback).catch(CommonNotice.errorFormatted);
  }

  public static getServiceGroup() {
    return Request.get(urlBase + '/groups', {});
  }

  /**
   * 启动服务
   * @param services
   * @param callback
   */
  public static startService(services: ServiceInstance[], callback: any) {
    const param = ServiceManager.parseParam(services);
    Request.post(`${urlBase}/startService`, param).then(callback);
  }

  /**
   * 终止服务
   * @param services
   * @param callback
   */
  public static stopService(services: ServiceInstance[], callback: any) {
    const param = ServiceManager.parseParam(services);
    Request.post(`${urlBase}/stopService`, param).then(callback);
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
    Request.get(`${urlBase}/oneClickRestart`, {}).then(CommonUtils.requestFinishCallback);
  }

  /**
   * 一键启动
   */
  public static oneClickStart() {
    Request.get(`${urlBase}/oneClickStart`, {}).then(CommonUtils.requestFinishCallback);
  }

  /**
   * 一键停止
   */
  public static oneClickStop() {
    Request.get(`${urlBase}/oneClickStop`, {}).then(CommonUtils.requestFinishCallback).catch(CommonNotice.errorFormatted);
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
    return Request.get(`${urlBase}/jvmProcesses`, {});
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
      if (!value.children?.length) {
        set.add(value.name as string);
        return;
      }
      const children = value.children as ServiceInstance[];
      children?.length && children.forEach(child => set.add(child.name as string));
    });
    return Array.from(set);
  }
}
