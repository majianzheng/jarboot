import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

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
     * @param userName
     * @param password
     */
    public static login(userName:string, password:string) {
        let form :FormData = new FormData();
        form.append("userName", userName);
        form.append("password", password);
        Request.post(`${urlBase}/login`, form)
            .then(resp => {
                if (resp.resultCode !== 0) {
                    CommonNotice.error(ErrorUtil.formatErrResp(resp));
                    return;
                }
                localStorage.setItem("token", resp.result);
                location.assign("/");
            })
            .catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    }
}
