import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";
import StringUtil from "@/common/StringUtil";
import React from "react";

const urlBase = "/api/jarboot/services";

interface ServerRunning {
    name: string,
    sid: string,
    status: string,
    path: string,
    group: string,
}

interface JvmProcess {
    name: string,
    fullName: string,
    pid: number,
    attached: boolean
}

interface TreeNode extends ServerRunning {
    title: string;
    key: string;
    isLeaf?: boolean;
    children?: TreeNode[];
    icon?: React.ReactNode;
}

export { ServerRunning, JvmProcess, TreeNode };
/**
 * 服务管理
 */
export default class ServerMgrService {

    /**
     * 获取服务列表
     * @param callback
     */
    public static getServerList(callback: any) {
        Request.get(`${urlBase}/getServerList`, {})
            .then(callback)
            .catch(CommonNotice.errorFormatted);
    }

    /**
     * 启动服务
     * @param servers
     * @param callback
     */
    public static startServer(servers: TreeNode[], callback: any) {
        const param = ServerMgrService.parseParam(servers);
        Request.post(`${urlBase}/startServer`, param).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 终止服务
     * @param servers
     * @param callback
     */
    public static stopServer(servers: TreeNode[], callback: any) {
        const param = ServerMgrService.parseParam(servers);
        Request.post(`${urlBase}/stopServer`, param).then(callback).catch(CommonNotice.errorFormatted);
    }

    /**
     * 重启服务
     * @param servers
     * @param callback
     */
    public static restartServer(servers: TreeNode[], callback: any) {
        const param = ServerMgrService.parseParam(servers);
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

    /**
     * 获取未被服务管理的JVM进程信息
     * @param callback
     */
    public static getJvmProcesses(callback: any) {
        Request.get(`${urlBase}/getJvmProcesses`, {})
            .then(callback)
            .catch(CommonNotice.errorFormatted);
    }

    /**
     * attach进程
     * @param pid pid
     * @param name 名字
     */
    public static attach(pid: number, name: string) {
        return Request.get(`${urlBase}/attach`, {pid, name});
    }

    /**
     * 删除服务
     * @param server 服务名
     */
    public static deleteServer(server: string) {
        return Request.get(`${urlBase}/deleteServer`, {server});
    }

    private static parseParam(servers: TreeNode[]): string[] {
        const set = new Set<string>();
        servers.forEach(value => {
            if (value.isLeaf) {
                set.add(value.path);
                return;
            }
            value.children?.length && value.children.forEach(child => set.add(child.path));
        });
        return Array.from(set);
    }
}
