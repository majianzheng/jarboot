import Request from "@/common/Request";

const urlBase = "/api/auth";

//暂未实现
export default class OAuthService {
    /**
     * 获取当前用户
     */
    public static getCurrentUser() {
        let token = localStorage.getItem("token");
        if (null === token) {
            token = '';
        }
        return Request.get(`${urlBase}/getCurrentUser?token=${token}`, {});
    }

    /**
     * 登录
     * @param username
     * @param password
     */
    public static login(username?:string, password?:string) {
        let form :FormData = new FormData();
        if (username && username.length > 0) {
            form.append("username", username);
        }
        if (password && password.length > 0) {
            form.append("password", password);
        }
        return Request.post(`${urlBase}/login`, form);
    }
}
