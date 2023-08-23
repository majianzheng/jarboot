package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.entity.RoleInfo;

import java.util.List;

/**
 * Role Service
 * @author majianzheng
 */
public interface RoleService {

    /**
     * get roles by page.
     *
     * @param role role
     * @param name role name
     * @param pageNo pageNo
     * @param pageSize pageSize
     * @return roles page info
     */
    PagedList<RoleInfo> getRoles(String role, String name, int pageNo, int pageSize);

    /**
     * assign role.
     *
     * @param role role
     * @param name name
     */
    void addRole(String role, String name);

    /**
     * 设置角色名
     * @param role 角色
     * @param name 名出
     */
    void setRoleName(String role, String name);

    /**
     * delete role.
     *
     * @param role role
     */
    void deleteRole(String role);


    /**
     * fuzzy query roles by role name.
     *
     * @param role role
     * @return roles
     */
    List<String> findRolesLikeRoleName(String role);

    /**
     * 获取角色列表
     * @return
     */
    List<RoleInfo> getRoleList();
}
