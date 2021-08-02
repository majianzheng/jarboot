import {extend, RequestOptionsInit} from 'umi-request';
// @ts-ignore
import Qs from 'qs';
import CommonUtils from "@/common/CommonUtils";
import Logger from "@/common/Logger";

export default class Request {
    private static request = extend({
        timeout: 30000,
        //'prefix' 前缀，统一设置 url 前缀
        prefix: '',
        //'suffix' 后缀，统一设置 url 后缀
        suffix: '',
        // 'credentials' 发送带凭据的请求
        credentials: 'include', // default
        // 'useCache' 是否使用缓存，当值为 true 时，GET 请求在 ttl 毫秒内将被缓存，缓存策略唯一 key 为 url + params 组合
        useCache: false, // default
        // 'ttl' 缓存时长（毫秒）， 0 为不过期
        ttl: 60000,
        // 'maxCache' 最大缓存数， 0 为无限制
        maxCache: 0,

        // 'paramsSerializer' 开发者可通过该函数对 params 做序列化（注意：此时传入的 params 为合并了 extends 中
        //  params 参数的对象，如果传入的是 URLSearchParams 对象会转化为 Object 对象
        paramsSerializer: params => {
            return Qs.stringify(params);
        },
        charset: 'utf8',
        responseType: 'json', //default
        errorHandler: error => {
            Logger.error(error);
            let {response} = error;
            if (401 === response.status) {
                //没有授权，跳转到登录界面
                Promise.resolve().then(() => {
                    CommonUtils.loginPage();
                });
            }
            return response;
        },
    });

    /**
     *
     * @param url
     * @param params 请求参数Map
     */
    static get(url: string, params: any) {
        return this.request.get(url, {params: params,});
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static post(url: string, params: any) {
        return this.request.post(url, {data: params,});
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static put(url: string, params: any) {
        return this.request.put(url, {data: params,});
    }

    /**
     *
     * @param url 请求地址
     * @param params 请求参数
     */
    static delete(url: string, params: any) {
        return this.request.delete(url, {data: params,});
    }

    public static init() {
        // 请求拦截器，塞入token以便鉴权
        this.request.interceptors.request.use( (url: string, options: RequestOptionsInit) => {
            let token = CommonUtils.getToken();

            const headers: any = options?.headers;
            if (headers && token.length) {
                headers['Authorization'] = token;
            }
            return {url, options};
        });
    }
}

Request.init();
