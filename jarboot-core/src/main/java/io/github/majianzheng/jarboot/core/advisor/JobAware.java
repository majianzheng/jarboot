package io.github.majianzheng.jarboot.core.advisor;

/**
 * 根据唯一id判定命令是否执行完成
 * @author majianzheng
 */
public interface JobAware {

    /**
     * 设置Job id
     * @param id job id
     */
    void setJobId(String id);

    /**
     * 获取Job id
     * @return job id
     */
    String getJobId();

    /**
     * 设置会话id
     * @param id 会话id
     */
    void setSessionId(String id);

    /**
     * 获取会话id
     * @return 会话id
     */
    String getSessionId();
}
