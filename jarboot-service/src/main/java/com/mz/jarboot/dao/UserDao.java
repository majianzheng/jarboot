package com.mz.jarboot.dao;

import com.mz.jarboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<User, Long> {

    User findFirstByUsername(String username);
}
