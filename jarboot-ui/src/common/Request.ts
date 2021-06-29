import {extend, RequestOptionsInit} from 'umi-request';
// @ts-ignore
import Qs from 'qs';

export default class Request {
    private static codeMessage = {
        200: '服务器成功返回请求的数据',
        201: '新建或修改数据成功',
        202: '一个请求已经进入后台排队（异步任务）',
        204: '删除数据成功',
        400: '发出的请求有错误，服务器没有进行新建或修改数据的操作',
        401: '用户没有权限（令牌、用户名、密码错误）',
        403: '用户得到授权，但是访问是被禁止的。',
        404: '发出的请求针对的是不存在的记录，服务器没有进行操作',
        406: '请求的格式不可得',
        410: '请求的资源被永久删除，且不会再得到的',
        422: '当创建一个对象时，发生一个验证错误',
        500: '服务器发生错误，请检查服务器',
        502: '网关错误',
        503: '服务不可用，服务器暂时过载或维护',
        504: '网关超时',
    };

    private static request = extend({
        timeout: 30000,
        //'prefix' 前缀，统一设置 url 前缀
        prefix: '',
        //'suffix' 后缀，统一设置 url 后缀
        suffix: '',
        // 'credentials' 发送带凭据的请求
        // 为了让浏览器发送包含凭据的请求（即使是跨域源），需要设置 credentials: 'include'
        // 如果只想在请求URL与调用脚本位于同一起源处时发送凭据，请添加credentials: 'same-origin'
        // 要改为确保浏览器不在请求中包含凭据，请使用credentials: 'omit'
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
            let errorMsg = '';
            let {response} = error;
            if (response && response.status) {
                // @ts-ignore
                errorMsg = Request.codeMessage[response.status] || response.statusText;
            } else if (!response) {
                errorMsg = '您的网络发生异常，无法连接服务器';
            }
            // @ts-ignore
            response.resultMsg = errorMsg;
            // @ts-ignore
            response.resultCode = -9999;
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
            let token = localStorage.getItem("token");
            if (!token) {
                token = '';
            }
            if (options?.method === 'post' ||
                options?.method === 'put' ||
                options?.method === 'delete' ||
                options?.method === 'get') {
                const headers: any = options?.headers;
                if (headers) {
                    headers['token'] = token;
                }
            }
            return {url, options};
        });
    }
}

Request.init();
