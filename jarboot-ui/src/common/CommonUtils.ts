import {CommonConst} from "@/common/CommonConst";

/**
 * @author majianzheng
 */
export default class CommonUtils {
    private static readonly HOME_PREFIX = '/jarboot/';
    private static readonly TOKEN_PREFIX = "Bearer ";
    public static readonly ACCESS_TOKEN = 'accessToken';

    public static loginPage() {
        localStorage.removeItem(CommonConst.TOKEN_KEY);
        if (0 === window.location.pathname.indexOf(CommonUtils.HOME_PREFIX)) {
            location.assign('/jarboot/login.html');
            return;
        }
        location.assign('/login.html');
    }

    public static homePage() {
        if (0 === window.location.pathname.indexOf(CommonUtils.HOME_PREFIX)) {
            location.assign('/jarboot/index.html');
            return;
        }
        location.assign('/');
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
}
