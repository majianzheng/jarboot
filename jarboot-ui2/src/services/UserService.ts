import Request from "@/common/Request";

const urlBase = "/api/jarboot/user";

/**
 * 用户管理操作
 */
export default class UserService {
    public static createUser(username:string, password:string) {
        let form :FormData = new FormData();
        form.append("username", username);
        form.append("password", password);
        return Request.post(urlBase, form);
    }

    public static deleteUser(id:number) {
        let form :FormData = new FormData();
        form.append("id", '' + id);
        return Request.delete(urlBase, form);
    }

    public static updateUserPassword(username:string, password:string) {
        let form :FormData = new FormData();
        form.append("username", username);
        form.append("password", password);
        return Request.put(urlBase, form);
    }

    public static getUsers(pageNo:number, pageSize:number) {
        return Request.get(`${urlBase}/getUsers`, {pageNo, pageSize});
    }
}
