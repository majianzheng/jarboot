import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import {requestFinishCallback} from "@/common/JarBootConst";

const urlBase = "/jarboot-service";

export default class ServerMgrService {

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
        ).then(requestFinishCallback).catch(error => {
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

    /**
     * 一键启动
     */
    public static oneClickStart() {
        Request.get(`${urlBase}/oneClickStart`, {}
        ).then(requestFinishCallback).catch(error => {
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }

    /**
     * 一键停止
     */
    public static oneClickStop() {
        Request.get(`${urlBase}/oneClickStop`, {}
        ).then(requestFinishCallback).catch(error => {
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
        let form = new FormData();
        form.append("server", server);
        form.append("command", command);
        Request.post(`${urlBase}/sendCommand`, form
        ).then(resp => {
            callback && callback();
            if (resp.resultCode === 0) {
                CommonNotice.info(`命令${command}执行成功 ${resp.body}`);
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => {
            callback && callback();
            CommonNotice.error(ErrorUtil.formatErrResp(error));
        });
    }
}
