package com.mz.jarboot.dao;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.exception.MzException;
import com.mz.jarboot.constant.CommonConst;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Date;

@Component
public class TaskRunDao {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private TaskRunFile taskRunFile = null;
    public void setTaskInfo(String name, String status, Integer pid) {
        TaskRunFile cache = this.load();
        cache.setTaskInfo(name, status, pid);
        this.update(cache);
    }
    public String getTaskStatus(String name) {
        return this.load().getTaskStatus(name);
    }
    public boolean hasNotFinished() {
        return this.load().hasNotFinished();
    }
    public Date getTaskStartedTime(String name) {
        return this.load().getTaskStartTime(name);
    }
    public Integer getTaskPid(String name) {
        return this.load().getTaskPid(name);
    }
    public Date getActionTime(String name) {
        return this.load().getActionTime(name);
    }
    private TaskRunFile load() {
        if (null != this.taskRunFile) {
            return this.taskRunFile;
        }
        TaskRunFile cache = new TaskRunFile();
        File cacheFile = FileUtils.getFile(this.getCacheFilePath());
        if (!cacheFile.exists() || !cacheFile.isFile()) {
            return cache;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            cache = (TaskRunFile) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return cache;
    }
    private void update(TaskRunFile cache) {
        File cacheFile = FileUtils.getFile(this.getCacheFilePath());
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(cacheFile));){
            if (cacheFile.isDirectory()) {
                FileUtils.deleteDirectory(cacheFile);
            }
            oo.writeObject(cache);
            //再更新到内存
            this.taskRunFile = cache;
        } catch (Exception e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "更新缓存文件失败！", e);
        }
    }
    private String getCacheFilePath() {
        String path = System.getProperty(CommonConst.WORKSPACE_HOME);
        StringBuilder builder = new StringBuilder();
        builder.append(path).append(File.separatorChar).append("taskRun.temp");
        return builder.toString();
    }
}
