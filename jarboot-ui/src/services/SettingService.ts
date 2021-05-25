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
    public static getServerList(callback: any, errorCallBack: any) {
        Request.get(`${urlBase}/getServerList`, {}).then(callback).catch(errorCallBack);
    }

    /**
     * 启动服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    public static startServer(param: any, callback: any, errorCallBack: any) {
        Request.post(`${urlBase}/startServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 终止服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    public static stopServer(param: any, callback: any, errorCallBack: any) {
        Request.post(`${urlBase}/stopServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 重启服务
     * @param param
     * @param callback
     * @param errorCallBack
     */
    public static restartServer(param: any, callback: any, errorCallBack: any) {
        Request.post(`${urlBase}/restartServer`, param).then(callback).catch(errorCallBack);
    }

    /**
     * 一键重启
     */
    public static oneClickRestart() {
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
    public static oneClickStart() {
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
    public static oneClickStop() {
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

    public static sendCommand(server: any, command: any, callback: any) {
        command = command.trim();
        if (command.length < 1) {
            return;
        }
        if (null === server || server.length < 1) {
            CommonNotice.info(`请先选择发送指令的服务，当前未选中任何服务！`);
            return;
        }
        let p = command.indexOf(' ');
        let cmd: any, param: any;
        if (-1 === p) {
            cmd = command;
            param = null;
        } else {
            cmd = command.substring(0, p);
            param = command.substring(p + 1);
        }
        let params = {cmd, param, ack: true,};
        Request.post(`${urlBase}/sendCommand?server=${server}`, params
        ).then(resp => {
            callback && callback();
            if (resp.resultCode === 0) {
                CommonNotice.info(`命令${cmd}执行成功 ${resp.body}`);
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => {
            callback && callback();
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

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
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
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
        ).then(resp => {
            if (resp.resultCode === 0) {
                CommonNotice.info('请求成功');
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
