import CommonConst from "./CommonConst";
import {getCurrentInstance} from "vue";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

/**
 * @author majianzheng
 */
export default class CommonUtils {
    private static readonly HOME_PREFIX = '/jarboot/';
    private static readonly TOKEN_PREFIX = "Bearer ";
    public static readonly ACCESS_TOKEN = 'accessToken';
    private static t: any;
    public static init() {
        CommonUtils.t = getCurrentInstance()?.appContext.config.globalProperties.$t;
    }

    public static translate(s : string) {
        return CommonUtils.t ? CommonUtils.t(s) : s;
    }
    public static getToken(): string {
        let token = localStorage.getItem(CommonConst.TOKEN_KEY);
        if (!token) {
            token = '';
        }
        return token;
    }

    public static storeToken(token: string) {
        if (0 !== token.indexOf(CommonUtils.TOKEN_PREFIX)) {
            token = CommonUtils.TOKEN_PREFIX + token;
        }
        localStorage.setItem(CommonConst.TOKEN_KEY, token);
    }

    public static deleteToken() {
        localStorage.removeItem(CommonConst.TOKEN_KEY);
    }

    public static getRawToken(): string {
        let token = localStorage.getItem(CommonConst.TOKEN_KEY);
        if (!token) {
            return '';
        }
        if (0 === token.indexOf(CommonUtils.TOKEN_PREFIX)) {
            token = token.substring(CommonUtils.TOKEN_PREFIX.length);
        }
        return token;
    }

    public static exportServer(name: string): void {
        const a = document.createElement('a');
        const token = CommonUtils.getRawToken();
        a.href = `/api/jarboot/cloud/pull/server?name=${name}&${CommonUtils.ACCESS_TOKEN}=${token}`;
        a.click();
    }

    public static requestFinishCallback = (resp: any) => {
        if (resp.resultCode !== 0) {
            CommonNotice.error(ErrorUtil.formatErrResp(resp));
        }
    };
}
