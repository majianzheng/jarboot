package com.mz.jarboot.dao;

import com.mz.jarboot.entity.TaskRunInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRunInfoDao extends JpaRepository<TaskRunInfo, Long> {

    TaskRunInfo findFirstByName(String username);
}
