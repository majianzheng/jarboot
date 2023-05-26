package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.pojo.ResponseForList;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.ROLE_CONTEXT)
@RestController
@Permission
public class RoleController {
    @Autowired
    private RoleService roleService;

    /**
     * 分配角色
     * @param role 角色
     * @param username 用户名
     * @return 执行结果
     */
    @PutMapping
    @ResponseBody
    @Permission("Add Role")
    public ResponseVo<String> addRole(String role, String username) {
        roleService.addRole(role, username);
        return HttpResponseUtils.success();
    }

    /**
     * 搜索角色
     * @param role 角色关键字
     * @return 角色列表
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseVo<List<String>> searchRoles(@RequestParam String role) {
        return HttpResponseUtils.success(roleService.findRolesLikeRoleName(role));
    }

    /**
     * 删除角色
     * @param role 角色
     * @param username 用户名
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    @Permission("Delete Role")
    public ResponseVo<String> deleteRole(@RequestParam String role,
                                     @RequestParam(name = "username", defaultValue = StringUtils.EMPTY) String username) {
        if (StringUtils.isBlank(username)) {
            roleService.deleteRole(role);
        } else {
            roleService.deleteRole(role, username);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 获取角色信息列表
     * @param pageNo 页数
     * @param pageSize 页大小
     * @return 角色信息列表
     */
    @GetMapping(value="/getRoles")
    @ResponseBody
    public ResponseForList<RoleInfo> getRoles(Integer pageNo, Integer pageSize) {
        return roleService.getRoles(pageNo, pageSize);
    }

    /**
     * 获取角色列表
     * @return 角色名列表
     */
    @GetMapping(value="/getRoleList")
    @ResponseBody
    public ResponseForList<String> getRoleList() {
        return new ResponseForList<>(roleService.getRoleList());
    }
}
