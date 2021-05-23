import Request from '../common/Request';
import ErrorUtil from "../common/ErrorUtil";
import CommonNotice from "../common/CommonNotice";

const urlBase = "/jarboot-service";
const settingUrl = "/jarboot-setting";

export default class SettingService {
    /**
     * 获取服务列表
     * @param callback
     * @param errorCallBack
     */
    static getServerList(callback, errorCallBack) {
        Request.get(`${urlBase}/getServerList`, {}).then(callback).catch(errorCallBack);
    }

    /**
     * 启动服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    static startServer(param, callback, errorCallBack) {
        Request.post(`${urlBase}/startServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 终止服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    static stopServer(param, callback, errorCallBack) {
        Request.post(`${urlBase}/stopServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 重启服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    static restartServer(param, callback, errorCallBack) {
        Request.post(`${urlBase}/restartServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 一键重启
     */
    static oneClickRestart() {
        Request.get(`${urlBase}/oneClickRestart`, {}
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => {
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

    /**
     * 一键启动
     */
    static oneClickStart() {
        Request.get(`${urlBase}/oneClickStart`, {}
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => {
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

    /**
     * 一键停止
     */
    static oneClickStop() {
        Request.get(`${urlBase}/oneClickStop`, {}
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => {
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

    /**
     * 获取服务配置
     * @param server
     * @returns {Promise<any>}
     */
    static getServerSetting(server) {
        return Request.get(`${settingUrl}/getServerSetting`, {});
    }

    /**
     * 提交服务配置
     * @param server 服务名
     * @param setting 配置信息
     */
    static submitServerSetting(server, setting) {
        Request.get(`${settingUrl}/submitServerSetting?server=${server}`, setting
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
