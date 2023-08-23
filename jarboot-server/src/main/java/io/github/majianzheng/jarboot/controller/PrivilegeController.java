package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.entity.Privilege;
import io.github.majianzheng.jarboot.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.PRIVILEGE_CONTEXT)
@RestController
public class PrivilegeController {
    @Autowired
    private PrivilegeService privilegeService;

    /**
     * 修改权限
     * @param role 角色
     * @param authCode 权限
     * @param permission 是否拥有权限
     * @return 执行结果
     */
    @PutMapping
    @ResponseBody
    public ResponseVo<String> savePrivilege(String role, String authCode, Boolean permission) {
        privilegeService.savePrivilege(role, authCode, permission);
        return HttpResponseUtils.success();
    }

    /**
     * 获取是否拥有权限
     * @param role 角色
     * @param authCode 权限
     * @return 是否拥有权限
     */
    @GetMapping
    @ResponseBody
    public ResponseVo<Boolean> hasPrivilege(String role, String authCode) {
        boolean has = privilegeService.hasPrivilege(role, authCode);
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
}
