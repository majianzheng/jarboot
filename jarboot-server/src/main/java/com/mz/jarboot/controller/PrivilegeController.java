package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.base.PermissionsCache;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.entity.Privilege;
import com.mz.jarboot.security.PermissionInfo;
import com.mz.jarboot.service.PrivilegeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

@Api(tags="权限管理")
@RequestMapping(value = "/api/jarboot-privilege")
@Controller
@Permission
public class PrivilegeController {
    @Autowired
    private PrivilegeService privilegeService;
    @Autowired
    private PermissionsCache permissionsCache;

    @ApiOperation(value = "修改权限", httpMethod = "PUT")
    @PutMapping
    @ResponseBody
    @Permission("Modify permission")
    public ResponseSimple savePrivilege(String role, String username, Boolean permission) {
        privilegeService.savePrivilege(role, username, permission);
        return new ResponseSimple();
    }

    @ApiOperation(value = "获取是否拥有权限", httpMethod = "GET")
    @GetMapping
    @ResponseBody
    public ResponseForObject<Boolean> hasPrivilege(String role, String username) {
        boolean has = privilegeService.hasPrivilege(role, username);
        return new ResponseForObject<>(has);
    }

    @ApiOperation(value = "获取是否拥有权限", httpMethod = "GET")
    @GetMapping("/getPrivilegeByRole")
    @ResponseBody
    public ResponseForList<Privilege> getPrivilegeByRole(String role) {
        List<Privilege> result = privilegeService.getPrivilegeByRole(role);
        return new ResponseForList<>(result);
    }

    @ApiOperation(value = "获取权限信息", httpMethod = "GET")
    @GetMapping("/getPermissionInfos")
    @ResponseBody
    public ResponseForList<PermissionInfo> getPermissionInfos() {
        List<PermissionInfo> result = permissionsCache.getPermissionInfos();
        return new ResponseForList<>(result);
    }
}
