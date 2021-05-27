import Request from '../common/Request';
import ErrorUtil from "../common/ErrorUtil";
import CommonNotice from "../common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";

const settingUrl = "/jarboot-setting";

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
        Request.post(`${settingUrl}/submitServerSetting?server=${server}`, setting
        ).then(requestFinishCallback).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
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
        Request.post(`${settingUrl}/submitGlobalSetting`, setting
        ).then(requestFinishCallback).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
