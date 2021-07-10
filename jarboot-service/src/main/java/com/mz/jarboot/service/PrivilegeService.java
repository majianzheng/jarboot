package com.mz.jarboot.service;

import com.mz.jarboot.entity.Privilege;

import java.util.List;

public interface PrivilegeService {
    /**
     * 获取角色的权限列表
     * @param role 角色
     * @return 权限列表
     */
    List<Privilege> getPrivilegeByRole(String role);

    /**
     * 保持权限
     * @param role 角色
     * @param resource 资源
     * @param permission 是否拥有权限
     */
    void savePrivilege(String role, String resource, Boolean permission);

    /**
     * 判断角色是否拥有权限
     * @param role 角色
     * @param resource 资源
     * @return 是否有权限
     */
    boolean hasPrivilege(String role, String resource);
}
