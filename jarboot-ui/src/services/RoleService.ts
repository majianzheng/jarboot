import Request from "@/common/Request";

const urlBase = "/api/jarboot-role";

/**
 * 用户管理操作
 */
export default class RoleService {
    public static addRole(role: string, username:string) {
        let form :FormData = new FormData();
        form.append("role", role);
        form.append("username", username);
        return Request.post(`${urlBase}/addRole`, form);
    }

    public static deleteRole(role: string, username: string) {
        return Request.delete(`${urlBase}/deleteRole?role=${role}&username=${username}`, {});
    }

    public static getRoles(pageNo:number, pageSize:number) {
        return Request.get(`${urlBase}/getRoles`, {pageNo, pageSize});
    }
}
