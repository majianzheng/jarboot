import Request from '../common/Request';

const settingUrl = "/api/jarboot/setting";

/**
 * 配置服务
 */
export default class SettingService {
    /**
     * 获取服务配置
     * @param path 服务路径
     * @returns {Promise<any>}
     */
    public static getServerSetting(path: string) {
        return Request.get(`${settingUrl}/serverSetting`, {path});
    }

    /**
     * 提交服务配置
     * @param setting 配置信息
     */
    public static submitServerSetting(setting: any) {
        return Request.post(`${settingUrl}/serverSetting`, setting);
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
     * @param path 服务路径
     * @param file vm内容
     * @returns {Promise<any>}
     */
    public static getVmOptions(path: string, file: string) {
        return Request.get(`${settingUrl}/vmoptions`, {path, file});
    }

    /**
     * 保存vm配置
     * @param path 服务路径
     * @param file 文件名
     * @param content vm内容
     */
    public static saveVmOptions(path: string, file: string, content: string) {
        let form = new FormData();
        form.append('path', path);
        form.append('file', file);
        form.append('content', content);
        return Request.post(`${settingUrl}/vmoptions`, form);
    }

    /**
     * 获取版本
     * @returns {Promise<any>}
     */
    public static getVersion() {
        return Request.get(`${settingUrl}/version`, {});
    }
}
