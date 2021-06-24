import Request from '../common/Request';
import ErrorUtil from "../common/ErrorUtil";
import CommonNotice from "../common/CommonNotice";
import {requestFinishCallback} from "@/common/JarBootConst";

const baseUrl = "/jarboot-upload";

export default class UploadFileService {
    /**
     * 开始上传服务的文件
     * @param server
     */
    public static beginUploadServerFile(server: string) {
        return Request.get(`${baseUrl}/beginUploadServerFile`, {server});
    }

    /**
     * 提交已经上传的文件
     * @param server
     */
    public static submitUploadFileInCache(server: string) {
        return Request.post(`${baseUrl}/submitUploadFileInCache?server=${server}`, {});
    }

    /**
     * 提交已经上传的文件
     * @param server
     * @param file
     */
    public static deleteUploadFileInCache(server: string, file: string) {
        return Request.delete(`${baseUrl}/deleteUploadFileInCache?server=${server}&file=${file}`, {});
    }

    /**
     * 提交已经上传的文件
     * @param server
     */
    public static clearUploadFileInCache(server: string) {
        Request.delete(`${baseUrl}/clearUploadFileInCache?server=${server}`, {})
            .then(requestFinishCallback)
            .catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
