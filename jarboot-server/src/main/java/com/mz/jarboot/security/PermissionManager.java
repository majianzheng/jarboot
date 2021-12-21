package com.mz.jarboot.security;

import com.mz.jarboot.base.PermissionsCache;
import com.mz.jarboot.common.ConcurrentWeakKeyHashMap;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.entity.RoleInfo;
import com.mz.jarboot.service.PrivilegeService;
import com.mz.jarboot.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Component
public class PermissionManager {
    @Autowired
    private PermissionsCache permissionsCache;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PrivilegeService privilegeService;

    private final ConcurrentWeakKeyHashMap<String, Set<String>> userRoleWeakMap = new ConcurrentWeakKeyHashMap<>();
    private final ConcurrentWeakKeyHashMap<String, Boolean> rolePrivilegeWeakMap = new ConcurrentWeakKeyHashMap<>();

    public boolean hasPermission(String username, HttpServletRequest request) {
        PermissionInfo permission = permissionsCache.getMethod(request);
        if (null == permission) {
            // 不需要鉴权的放行接口
            return true;
        }
        if (AuthConst.JARBOOT_USER.equals(username)) {
            // 最高权限用户无需判断
            return true;
        }
        // 获取role
        Set<String> roleSet = userRoleWeakMap.computeIfAbsent(username, user -> getRolesFromDb(username));
        if (CollectionUtils.isEmpty(roleSet)) {
            return false;
        }
        for (String role : roleSet) {
            if (AuthConst.ADMIN_ROLE.equals(role)) {
                return true;
            }
            String key = genRoleResourceKey(role, permission.getResource());
            Boolean hasPermission = rolePrivilegeWeakMap.computeIfAbsent(key,
                    p -> privilegeService.hasPrivilege(role, permission.getResource()));
            if (Boolean.TRUE.equals(hasPermission)) {
                return true;
            }
        }
        return false;
    }
    private Set<String> getRolesFromDb(String username) {
        ResponseForList<RoleInfo> resp = roleService.getRolesByUserName(username, 0, Integer.MAX_VALUE);
        if (0 != resp.getResultCode()) {
            return new HashSet<>();
        }
        List<RoleInfo> roles = resp.getResult();
        if (CollectionUtils.isEmpty(roles)) {
            return new HashSet<>();
        }
        return roles.stream().map(RoleInfo::getRole).collect(Collectors.toSet());
    }
    private String genRoleResourceKey(String role, String resource) {
        return role + " -> " + resource;
    }
}
