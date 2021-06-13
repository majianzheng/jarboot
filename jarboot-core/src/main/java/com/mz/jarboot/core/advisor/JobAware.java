package com.mz.jarboot.core.advisor;

/**
 * 根据唯一id判定命令是否执行完成
 */
public interface JobAware {

    void setJobId(String id);

    String getJobId();

    void setSessionId(String id);

    String getSessionId();
}
