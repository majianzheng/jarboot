package com.mz.jarboot.dao;

import com.mz.jarboot.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivilegeDao extends JpaRepository<Privilege, Long> {

    List<Privilege> findAllByRole(String role);

    Privilege findFirstByRoleAndResource(String role, String resource);
}
