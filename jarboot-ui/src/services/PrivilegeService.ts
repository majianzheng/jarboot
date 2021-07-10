import Request from "@/common/Request";

const urlBase = "/api/jarboot-privilege";

/**
 * 权限管理操作
 */
export default class PrivilegeService {
    public static savePrivilege(role: string, username:string, permission: boolean) {
        let form :FormData = new FormData();
        form.append("role", role);
        form.append("username", username);
        form.append("permission", '' + permission);
        return Request.put(urlBase, form);
    }

    public static hasPrivilege(role: string, username: string) {
        return Request.get(urlBase, {role, username});
    }

    public static getPrivilegeByRole(role: string) {
        return Request.get(`${urlBase}/getPrivilegeByRole`, {role});
    }

    public static getPermissionInfos() {
        return Request.get(`${urlBase}/getPermissionInfos`, {});
    }
}
