import {JarBootConst} from "@/common/JarBootConst";

/**
 * @author majianzheng
 */
export default class CommonUtils {
    public static loginPage() {
        localStorage.removeItem(JarBootConst.TOKEN_KEY);
        location.assign('/login.html');
    }
}
