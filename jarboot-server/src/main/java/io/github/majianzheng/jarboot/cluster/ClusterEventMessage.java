package io.github.majianzheng.jarboot.cluster;

/**
 * 集群事件消息
 * @author mazheng
 */
public class ClusterEventMessage {
    public static final int REQ_TYPE = 0;
    public static final int RSP_TYPE = 1;
    /** 唯一ID */
    private String id;
    /** 消息名称 */
    private String name;
    /** 0: 请求，1: 响应 */
    private int type;
    /** 是否需要回执，仅type为 0 时有效 */
    private boolean needAck;
    private String body;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isNeedAck() {
        return needAck;
    }

    public void setNeedAck(boolean needAck) {
        this.needAck = needAck;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
