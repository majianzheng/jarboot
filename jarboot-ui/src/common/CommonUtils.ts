import {JarBootConst} from "@/common/JarBootConst";

/**
 * @author majianzheng
 */
export default class CommonUtils {
    public static loginPage() {
        localStorage.removeItem(JarBootConst.TOKEN_KEY);
        location.assign('/login.html');
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
