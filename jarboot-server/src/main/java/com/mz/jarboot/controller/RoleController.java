package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags="角色管理")
@RequestMapping(value = "/api/jarboot/role")
@Controller
@Permission
public class RoleController {
    @Autowired
    private RoleService roleService;

    @ApiOperation(value = "分配角色", httpMethod = "PUT")
    @PutMapping
    @ResponseBody
    @Permission("Add Role")
    public ResponseSimple addRole(String role, String username) {
        roleService.addRole(role, username);
        return new ResponseSimple();
    }

    @ApiOperation(value = "搜索角色", httpMethod = "GET")
    @GetMapping("/search")
    @ResponseBody
    public List<String> searchRoles(@RequestParam String role) {
        return roleService.findRolesLikeRoleName(role);
    }

    @ApiOperation(value = "删除角色", httpMethod = "DELETE")
    @DeleteMapping
    @ResponseBody
    @Permission("Delete Role")
    public ResponseSimple deleteRole(@RequestParam String role,
                                     @RequestParam(name = "username", defaultValue = StringUtils.EMPTY) String username) {
        if (StringUtils.isBlank(username)) {
            roleService.deleteRole(role);
        } else {
            roleService.deleteRole(role, username);
        }
        return new ResponseSimple();
    }

    @ApiOperation(value = "获取角色列表", httpMethod = "GET")
    @GetMapping(value="/getRoles")
    @ResponseBody
    public ResponseForList<RoleInfo> getRoles(Integer pageNo, Integer pageSize) {
        return roleService.getRoles(pageNo, pageSize);
    }

    @ApiOperation(value = "获取角色列表", httpMethod = "GET")
    @GetMapping(value="/getRoleList")
    @ResponseBody
    public ResponseForList<String> getRoleList() {
        return new ResponseForList<>(roleService.getRoleList());
    }
}
