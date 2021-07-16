import Request from '../common/Request';

const settingUrl = "/api/jarboot-setting";

export default class SettingService {
    /**
     * 获取服务配置
     * @param server
     * @returns {Promise<any>}
     */
    public static getServerSetting(server: string) {
        return Request.get(`${settingUrl}/getServerSetting`, {server});
    }

    /**
     * 提交服务配置
     * @param server 服务名
     * @param setting 配置信息
     */
    public static submitServerSetting(server: string, setting: any) {
        return Request.post(`${settingUrl}/submitServerSetting?server=${server}`, setting);
    }

    /**
     * 获取服务配置
     * @returns {Promise<any>}
     */
    public static getGlobalSetting() {
        return Request.get(`${settingUrl}/getGlobalSetting`, {});
    }

    /**
     * 提交服务配置
     * @param setting 配置信息
     */
    public static submitGlobalSetting(setting: any) {
        return  Request.post(`${settingUrl}/submitGlobalSetting`, setting);
    }

    /**
     * 获取vm配置
     * @returns {Promise<any>}
     */
    public static getVmOptions(server: string, file: string) {
        return Request.get(`${settingUrl}/vmoptions`, {server, file});
    }

    /**
     * 保存vm配置
     * @param setting 配置信息
     */
    public static saveVmOptions(server: string, file: string, content: string) {
        let form = new FormData();
        form.append('server', server);
        form.append('file', file);
        form.append('content', content);
        return Request.post(`${settingUrl}/vmoptions`, form);
    }
}
