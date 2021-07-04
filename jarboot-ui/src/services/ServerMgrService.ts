import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import {requestFinishCallback} from "@/common/JarBootConst";
import StringUtil from "@/common/StringUtil";

const urlBase = "/api/jarboot-service";

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

    /**
     * base64编码
     * @param data
     */
    public static base64Encoder(data: string) {
        if (StringUtil.isEmpty(data)) {
            return Promise.resolve();
        }
        return Request.get(`${urlBase}/base64Encoder`, {data});
    }
}
