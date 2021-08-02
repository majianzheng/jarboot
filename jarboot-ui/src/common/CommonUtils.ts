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
}
