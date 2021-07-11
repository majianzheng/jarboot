import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";
import StringUtil from "@/common/StringUtil";

const urlBase = "/api/jarboot-service";

export default class ServerMgrService {

    /**
     * 获取服务列表
     * @param callback
     */
    public static getServerList(callback: any) {
        Request.get(`${urlBase}/getServerList`, {}).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 启动服务
     * @param param
     * @param callback
     */
    public static startServer(param: any, callback: any) {
        Request.post(`${urlBase}/startServer`, param).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 终止服务
     * @param param
     * @param callback
     */
    public static stopServer(param: any, callback: any) {
        Request.post(`${urlBase}/stopServer`, param).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 重启服务
     * @param param
     * @param callback
     */
    public static restartServer(param: any, callback: any) {
        Request.post(`${urlBase}/restartServer`, param).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 一键重启
     */
    public static oneClickRestart() {
        Request.get(`${urlBase}/oneClickRestart`, {}
        ).then(requestFinishCallback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 一键启动
     */
    public static oneClickStart() {
        Request.get(`${urlBase}/oneClickStart`, {}
        ).then(requestFinishCallback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 一键停止
     */
    public static oneClickStop() {
        Request.get(`${urlBase}/oneClickStop`, {}
        ).then(requestFinishCallback).catch(CommonNotice.errorFormatted);
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
