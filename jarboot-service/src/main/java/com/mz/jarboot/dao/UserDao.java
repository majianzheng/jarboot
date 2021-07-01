package com.mz.jarboot.dao;

import com.mz.jarboot.entity.User;
import org.springframework.stereotype.Component;

public interface UserDao {
    void createUser(User user);
}
