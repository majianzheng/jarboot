package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.dao.PrivilegeDao;
import io.github.majianzheng.jarboot.dao.RoleDao;
import io.github.majianzheng.jarboot.entity.Privilege;
import io.github.majianzheng.jarboot.service.PrivilegeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Service
public class PrivilegeServiceImpl implements PrivilegeService {
    @Resource
    private PrivilegeDao privilegeDao;
    @Resource
    private RoleDao roleDao;
    @Override
    public List<Privilege> getPrivilegeByRole(String roles) {
        if (StringUtils.isEmpty(roles)) {
            throw new JarbootException("roles is empty");
        }
        Set<String> roleSet = Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet());
        // 权限合并
        return privilegeDao.findAllByRoleIn(roleSet);
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
