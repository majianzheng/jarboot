package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.dao.PrivilegeDao;
import com.mz.jarboot.entity.Privilege;
import com.mz.jarboot.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {
    @Autowired
    private PrivilegeDao privilegeDao;
    @Override
    public List<Privilege> getPrivilegeByRole(String role) {
        return privilegeDao.findAllByRole(role);
    }

    @Override
    public void savePrivilege(String role, String resource, Boolean permission) {
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            throw new MzException("Admin role can't modify!");
        }
        Privilege privilege = privilegeDao.findFirstByRoleAndResource(role, resource);
        if (null == privilege) {
            privilege = new Privilege();
            privilege.setRole(role);
            privilege.setResource(resource);
        }
        privilege.setPermission(permission);
        privilegeDao.save(privilege);
    }

    @Override
    public boolean hasPrivilege(String role, String resource) {
        if (AuthConst.ADMIN_ROLE.equals(role)) {
            return true;
        }
        Privilege privilege = privilegeDao.findFirstByRoleAndResource(role, resource);

        return null != privilege && Boolean.TRUE.equals(privilege.getPermission());
    }
}
