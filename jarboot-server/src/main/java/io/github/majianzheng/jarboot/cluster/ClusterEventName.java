package io.github.majianzheng.jarboot.cluster;

/**
 * @author mazheng
 */

public enum ClusterEventName {
    /** 推送前端 */
    NOTIFY_TO_CLUSTER,
    /** 方法执行 */
    EXEC_FUNC,
    /** 启动服务 */
    START_SERVICE,
    /** 停止服务 */
    STOP_SERVICE,
    /** 集群间鉴权 */
    CLUSTER_AUTH,
}
