import Request from '../common/Request';

const settingUrl = "/api/jarboot/setting";

/**
 * 配置服务
 */
export default class SettingService {
    /**
     * 获取服务配置
     * @param serviceName 服务名
     * @returns {Promise<any>}
     */
    public static getServerSetting(serviceName: string) {
        return Request.get(`${settingUrl}/serviceSetting`, {serviceName});
    }

    /**
     * 提交服务配置
     * @param setting 配置信息
     */
    public static submitServerSetting(setting: any) {
        return Request.post(`${settingUrl}/serviceSetting`, setting);
    }

    /**
     * 获取服务配置
     * @returns {Promise<any>}
     */
    public static getGlobalSetting() {
        return Request.get(`${settingUrl}/globalSetting`, {});
    }

    /**
     * 提交服务配置
     * @param setting 配置信息
     */
    public static submitGlobalSetting(setting: any) {
        return  Request.post(`${settingUrl}/globalSetting`, setting);
    }

    /**
     * 获取vm配置
     * @param serviceName 服务路径
     * @param file vm内容
     * @returns {Promise<any>}
     */
    public static getVmOptions(serviceName: string, file: string) {
        return Request.get(`${settingUrl}/vmoptions`, {serviceName, file});
    }

    /**
     * 保存vm配置
     * @param serviceName 服务路径
     * @param file 文件名
     * @param content vm内容
     */
    public static saveVmOptions(serviceName: string, file: string, content: string) {
        let form = new FormData();
        form.append('serviceName', serviceName);
        form.append('file', file);
        form.append('content', content);
        return Request.post(`${settingUrl}/vmoptions`, form);
    }

    /**
     * 添加信任的远程主机
     * @param host 地址
     */
    public static addTrustedHost(host: string) {
        let form = new FormData();
        form.append('host', host);
        return Request.post(`${settingUrl}/trustedHost`, form);
    }

    /**
     * 添加信任的远程主机
     * @param host 地址
     */
    public static removeTrustedHost(host: string) {
        let form = new FormData();
        form.append('host', host);
        return Request.delete(`${settingUrl}/trustedHost`, form);
    }

    /**
     * 添加信任的远程主机
     */
    public static getTrustedHosts() {
        return Request.get(`${settingUrl}/trustedHost`, {});
    }
}
