import Request from '../common/Request';
import ErrorUtil from "../common/ErrorUtil";
import CommonNotice from "../common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";

const baseUrl = "/api/jarboot/upload";

/**
 * 文件上传
 */
export default class UploadFileService {
    /**
     * Begin upload files
     * @param server
     */
    public static startUploadFile(server: string) {
        return Request.get(`${baseUrl}/start`, {server});
    }

    /**
     * upload file heartbeat
     * @param server
     * @returns {Promise<any>}
     */
    public static uploadHeartbeat(server: string) {
        return Request.get(`${baseUrl}/heartbeat`, {server});
    }

    /**
     * Submit upload files
     * @param settings 配置
     * @returns {Promise<any>}
     */
    public static submitUploadFile(settings: any) {
        return Request.post(`${baseUrl}/file`, settings);
    }

    /**
     * Delete files of uploaded.
     * @param server
     * @param file
     * @returns {Promise<any>}
     */
    public static deleteCacheFile(server: string, file: string) {
        return Request.delete(`${baseUrl}/file?server=${server}&file=${file}`, {});
    }

    /**
     * Clear files of uploaded.
     * @param server
     */
    public static clearUploadFileInCache(server: string) {
        Request.delete(`${baseUrl}?server=${server}`, {})
            .then(requestFinishCallback)
            .catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
