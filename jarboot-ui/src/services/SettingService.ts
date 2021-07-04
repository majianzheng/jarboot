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
}
