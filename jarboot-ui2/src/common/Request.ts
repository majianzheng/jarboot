import axios from "axios";
import CommonUtils from "./CommonUtils";
import router from '../router';
import CommonNotice from "@/common/CommonNotice";

const http = axios.create({
    baseURL: "",
    timeout: 30000,
});

/**
 * Http请求封装
 * @author majianzheng
 */
export default class Request {
    /**
     *
     * @param url
     * @param params 请求参数Map
     */
    static get(url: string, params: any) {
        return http.get<any, any>(url, {params: params,});
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static post(url: string, params: any) {
        return http.post<any, any>(url, params);
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static put(url: string, params: any) {
        return http.put<any, any>(url, params);
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static delete(url: string, params: any) {
        return http.delete<any, any>(url, params);
    }

    public static init() {
        http.interceptors.response.use(response => {
            if (401 === response?.status) {
                //没有授权，跳转到登录界面
                router.push({path: '/login'}).then(() => console.info('未登陆，跳转到登陆界面...')) ;
            }
            let data = response.data;
            if (typeof data == 'string' && (data.startsWith('{') || data.startsWith('['))) {
                try {
                    const temp = JSON.parse(data);
                    if (temp) {
                        data = temp;
                    }
                } catch (error) {
                    //
                }
            }
            const resultCode = data.resultCode ?? -1;
            if (data.hasOwnProperty('resultCode') && resultCode < 0) {
                return Promise.reject(data);
            }
            return Promise.resolve(data);
        }, error => {
            const msg = `请求${error.config.url}发生错误：`;
            console.error(msg, error);
            CommonNotice.error(msg);
            return Promise.reject(error);
        });

        // 请求拦截器，塞入token以便鉴权
        http.interceptors.request.use(request => {
            let token = CommonUtils.getToken();
            if (request.headers && token.length) {
                request.headers['Authorization'] = token;
            }
            return request;
        });
    }
}

Request.init();
