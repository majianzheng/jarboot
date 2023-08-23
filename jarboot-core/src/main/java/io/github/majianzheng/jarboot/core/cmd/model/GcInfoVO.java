package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * GC info of dashboard
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class GcInfoVO {
    private String name;
    private long collectionCount;
    private long collectionTime;

    public GcInfoVO(String name, long collectionCount, long collectionTime) {
        this.name = name;
        this.collectionCount = collectionCount;
        this.collectionTime = collectionTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCollectionCount() {
        return collectionCount;
    }

    public void setCollectionCount(long collectionCount) {
        this.collectionCount = collectionCount;
    }

    public long getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }
}
