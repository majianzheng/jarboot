import {JarBootConst} from "@/common/JarBootConst";

export default class CommonUtils {
    public static loginPage() {
        localStorage.removeItem(JarBootConst.TOKEN_KEY);
        location.assign('/login.html');
    }
}
