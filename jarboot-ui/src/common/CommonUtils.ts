import {JarBootConst} from "@/common/JarBootConst";

/**
 * @author majianzheng
 */
export default class CommonUtils {
    private static readonly HOME_PREFIX = '/jarboot/';
    public static loginPage() {
        localStorage.removeItem(JarBootConst.TOKEN_KEY);
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
        let token = localStorage.getItem(JarBootConst.TOKEN_KEY);
        if (!token) {
            token = '';
        }
        return token;
    }

    public static exportServer(name: string): void {
        const a = document.createElement('a');
        a.href = `/api/jarboot/cloud/pull/server?name=${name}`;
        a.click();
    }
}
