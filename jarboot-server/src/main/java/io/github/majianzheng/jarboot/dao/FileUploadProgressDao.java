package io.github.majianzheng.jarboot.dao;

import io.github.majianzheng.jarboot.entity.FileUploadProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author majianzheng
 */
@Repository
public interface FileUploadProgressDao extends JpaRepository<FileUploadProgress, Long> {
    /**
     * 根据dstPath获取上传进度
     * @param clusterHost 集群机器
     * @param dstPath 目的路径
     * @return 进度
     */
    FileUploadProgress getFileUploadProgressByClusterHostAndDstPath(String clusterHost, String dstPath);

    /**
     * 删除已完成的上传项
     */
    @Query("delete from FileUploadProgress where uploadSize>=totalSize")
    @Modifying
    @Transactional(rollbackFor = Throwable.class)
    void deleteFinished();
}
