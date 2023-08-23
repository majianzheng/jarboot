package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * Data model of TraceCommand
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class TraceModel extends ResultModel {
    private TraceNode root;
    private int nodeCount;

    public TraceModel() {
        //do nothing
    }

    public TraceModel(TraceNode root, int nodeCount) {
        this.root = root;
        this.nodeCount = nodeCount;
    }

    @Override
    public String getName() {
        return "trace";
    }

    public TraceNode getRoot() {
        return root;
    }

    public void setRoot(TraceNode root) {
        this.root = root;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }
}
