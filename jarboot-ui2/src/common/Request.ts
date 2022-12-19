import axios from "axios";
import CommonUtils from "./CommonUtils";
import router from '../router';

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
        return axios.get(url, {params: params,});
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static post(url: string, params: any) {
        return axios.post(url, params);
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static put(url: string, params: any) {
        return axios.put(url, params);
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static delete(url: string, params: any) {
        return axios.delete(url, params);
    }

    public static init() {
        axios.interceptors.response.use(response => {
            if (401 === response?.status) {
                //没有授权，跳转到登录界面
                router.push({path: '/login'}).then(() => console.info('未登陆，跳转到登陆界面...')) ;
            }
            return response.data as any;
        });
        // 请求拦截器，塞入token以便鉴权
        axios.interceptors.request.use(request => {
            let token = CommonUtils.getToken();
            if (request.headers && token.length) {
                request.headers['Authorization'] = token;
            }
            return request;
        });
    }
}

Request.init();
