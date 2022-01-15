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
     * @param serviceName
     */
    public static startUploadFile(serviceName: string) {
        return Request.get(`${baseUrl}/start`, {serviceName});
    }

    /**
     * upload file heartbeat
     * @param serviceName
     * @returns {Promise<any>}
     */
    public static uploadHeartbeat(serviceName: string) {
        return Request.get(`${baseUrl}/heartbeat`, {serviceName});
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
     * @param serviceName
     * @param file
     * @returns {Promise<any>}
     */
    public static deleteCacheFile(serviceName: string, file: string) {
        const form = new FormData();
        form.append("serviceName", serviceName);
        form.append("file", file);
        return Request.delete(`${baseUrl}/file`, form);
    }

    /**
     * Clear files of uploaded.
     * @param serviceName
     */
    public static clearUploadFileInCache(serviceName: string) {
        const form = new FormData();
        form.append("serviceName", serviceName);
        Request.delete(baseUrl, form)
            .then(requestFinishCallback)
            .catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
