package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.annotation.EnableAuditLog;
import io.github.majianzheng.jarboot.common.annotation.PrivilegeCheck;
import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.entity.RoleInfo;
import io.github.majianzheng.jarboot.service.RoleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.ROLE_CONTEXT)
@RestController
@PrivilegeCheck(value = "ROLE_MGR")
public class RoleController {
    @Resource
    private RoleService roleService;

    /**
     * 分配角色
     * @param role 角色
     * @param name 角色名
     * @return 执行结果
     */
    @PutMapping
    @EnableAuditLog("添加角色")
    public ResponseVo<String> addRole(String role, String name) {
        roleService.addRole(role, name);
        return HttpResponseUtils.success();
    }

    /**
     * 设置角色名
     * @param role 角色
     * @param name 角色名
     * @return 执行结果
     */
    @PutMapping("/name")
    @EnableAuditLog("设置角色名")
    public ResponseVo<String> setRoleName(String role, String name) {
        roleService.setRoleName(role, name);
        return HttpResponseUtils.success();
    }

    /**
     * 搜索角色
     * @param role 角色关键字
     * @return 角色列表
     */
    @GetMapping("/search")
    public ResponseVo<List<String>> searchRoles(@RequestParam String role) {
        return HttpResponseUtils.success(roleService.findRolesLikeRoleName(role));
    }

    /**
     * 删除角色
     * @param role 角色
     * @return 执行结果
     */
    @DeleteMapping
    @EnableAuditLog("删除角色")
    public ResponseVo<String> deleteRole(@RequestParam String role) {
        roleService.deleteRole(role);
        return HttpResponseUtils.success();
    }

    /**
     * 获取角色信息列表
     * @param role 角色
     * @param name 名称
     * @param pageNo 页数
     * @param pageSize 页大小
     * @return 角色信息列表
     */
    @GetMapping(value="/getRoles")
    public ResponseVo<PagedList<RoleInfo>> getRoles(String role, String name, Integer pageNo, Integer pageSize) {
        return HttpResponseUtils.success(roleService.getRoles(role, name, pageNo, pageSize));
    }

    /**
     * 获取角色列表
     * @return 角色名列表
     */
    @GetMapping(value="/getRoleList")
    public ResponseVo<List<RoleInfo>> getRoleList() {
        return HttpResponseUtils.success(roleService.getRoleList());
    }
}
