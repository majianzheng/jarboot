package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.PrivilegeDao;
import com.mz.jarboot.dao.RoleDao;
import com.mz.jarboot.entity.Privilege;
import com.mz.jarboot.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Service
public class PrivilegeServiceImpl implements PrivilegeService {
    @Autowired
    private PrivilegeDao privilegeDao;
    @Autowired
    private RoleDao roleDao;
    @Override
    public List<Privilege> getPrivilegeByRole(String roles) {
        if (StringUtils.isEmpty(roles)) {
            throw new JarbootException("roles is empty");
        }
        Set<String> roleSet = Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet());
        // 权限合并
        List<Privilege> privileges = new ArrayList<>();
        for (String role : roleSet) {
            List<Privilege> temp = privilegeDao.findAllByRole(role);
            if (null != temp && !temp.isEmpty()) {
                privileges.addAll(temp);
            }
        }
        return privileges;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void savePrivilege(String role, String authCode, Boolean permission) {
        if (AuthConst.SYS_ROLE.equals(role)) {
            throw new JarbootException("SYSTEM role privilege can't modify!");
        }
        if (!roleDao.existsByRole(role)) {
            throw new JarbootException("Role is not exist!");
        }
        Privilege privilege = privilegeDao.findFirstByRoleAndAuthCode(role, authCode);
        if (null == privilege) {
            privilege = new Privilege();
            privilege.setRole(role);
            privilege.setAuthCode(authCode);
        }
        privilege.setPermission(permission);
        privilegeDao.save(privilege);
    }

    @Override
    public boolean hasPrivilege(String role, String authCode) {
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            return true;
        }
        Privilege privilege = privilegeDao.findFirstByRoleAndAuthCode(role, authCode);

        return null != privilege && Boolean.TRUE.equals(privilege.getPermission());
    }
}
