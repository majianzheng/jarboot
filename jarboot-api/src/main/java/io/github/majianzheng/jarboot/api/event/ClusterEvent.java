package io.github.majianzheng.jarboot.api.event;

/**
 * Cluster event
 * @author majianzheng
 */
public class ClusterEvent implements JarbootEvent {
    private boolean flag = true;

    public void marked() {
        flag = false;
    }

    public boolean canNotify() {
        return flag;
    }
}
