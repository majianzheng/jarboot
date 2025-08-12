package io.github.majianzheng.jarboot.api.constant;

/**
 * 集群服务状态
 * @author mazheng
 */
public enum ClusterServerState {
    /** 离线 */
    OFFLINE,

    /** 在线 */
    ONLINE,

    /** 认证失败，集群服务间cluster-secret-key配置不一致 */
    AUTH_FAILED,
}
