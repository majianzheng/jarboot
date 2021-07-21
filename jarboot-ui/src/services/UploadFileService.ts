import Request from '../common/Request';
import ErrorUtil from "../common/ErrorUtil";
import CommonNotice from "../common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";

const baseUrl = "/api/jarboot/upload";

export default class UploadFileService {
    /**
     * Begin upload files
     * @param server
     */
    public static beginUploadServerFile(server: string) {
        return Request.get(`${baseUrl}/beginUploadServerFile`, {server});
    }

    /**
     * upload file heartbeat
     * @param server
     * @returns {Promise<any>}
     */
    public static uploadServerHeartbeat(server: string) {
        return Request.get(`${baseUrl}/uploadServerHeartbeat`, {server});
    }

    /**
     * Submit upload files
     * @param server
     * @returns {Promise<any>}
     */
    public static submitUploadFileInCache(server: string) {
        return Request.post(`${baseUrl}/submitUploadFileInCache?server=${server}`, {});
    }

    /**
     * Delete files of uploaded.
     * @param server
     * @param file
     * @returns {Promise<any>}
     */
    public static deleteFileInUploadCache(server: string, file: string) {
        return Request.delete(`${baseUrl}/deleteFileInUploadCache?server=${server}&file=${file}`, {});
    }

    /**
     * Clear files of uploaded.
     * @param server
     */
    public static clearUploadFileInCache(server: string) {
        Request.delete(`${baseUrl}/clearUploadFileInCache?server=${server}`, {})
            .then(requestFinishCallback)
            .catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
