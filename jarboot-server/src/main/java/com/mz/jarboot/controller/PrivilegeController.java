package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.base.PermissionsCache;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.entity.Privilege;
import com.mz.jarboot.security.PermissionInfo;
import com.mz.jarboot.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.PRIVILEGE_CONTEXT)
@RestController
@Permission
public class PrivilegeController {
    @Autowired
    private PrivilegeService privilegeService;
    @Autowired
    private PermissionsCache permissionsCache;

    /**
     * 修改权限
     * @param role 角色
     * @param username 用户名
     * @param permission 是否拥有权限
     * @return 执行结果
     */
    @PutMapping
    @ResponseBody
    @Permission("Modify permission")
    public ResponseVo<String> savePrivilege(String role, String username, Boolean permission) {
        privilegeService.savePrivilege(role, username, permission);
        return HttpResponseUtils.success();
    }

    /**
     * 获取是否拥有权限
     * @param role 角色
     * @param username 用户
     * @return 是否拥有权限
     */
    @GetMapping
    @ResponseBody
    public ResponseVo<Boolean> hasPrivilege(String role, String username) {
        boolean has = privilegeService.hasPrivilege(role, username);
        return HttpResponseUtils.success(has);
    }

    /**
     * 根据角色获取权限
     * @param role 角色
     * @return 权限列表
     */
    @GetMapping("/getPrivilegeByRole")
    @ResponseBody
    public ResponseVo<List<Privilege>> getPrivilegeByRole(String role) {
        List<Privilege> result = privilegeService.getPrivilegeByRole(role);
        return HttpResponseUtils.success(result);
    }

    /**
     * 获取权限信息
     * @return 权限信息列表
     */
    @GetMapping("/getPermissionInfos")
    @ResponseBody
    public ResponseVo<List<PermissionInfo>> getPermissionInfos() {
        List<PermissionInfo> result = permissionsCache.getPermissionInfos();
        return HttpResponseUtils.success(result);
    }
}
