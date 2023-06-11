package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.PrivilegeDao;
import com.mz.jarboot.entity.Privilege;
import com.mz.jarboot.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author majianzheng
 */
@Service
public class PrivilegeServiceImpl implements PrivilegeService {
    @Autowired
    private PrivilegeDao privilegeDao;
    @Override
    public List<Privilege> getPrivilegeByRole(String role) {
        return privilegeDao.findAllByRole(role);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void savePrivilege(String role, String authCode, Boolean permission) {
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            throw new JarbootException("Admin role can't modify!");
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
