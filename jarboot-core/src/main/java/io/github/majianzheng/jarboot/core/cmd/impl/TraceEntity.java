package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.cmd.model.TraceModel;
import io.github.majianzheng.jarboot.core.cmd.model.TraceTree;
import io.github.majianzheng.jarboot.core.utils.ThreadUtil;

/**
 * 用于在ThreadLocal中传递的实体
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class TraceEntity {

    protected TraceTree tree;
    protected int deep;

    public TraceEntity(ClassLoader loader) {
        this.tree = createTraceTree(loader);
        this.deep = 0;
    }

    public int getDeep() {
        return deep;
    }

    public void setDeep(int deep) {
        this.deep = deep;
    }

    private TraceTree createTraceTree(ClassLoader loader) {
        return new TraceTree(ThreadUtil.getThreadNode(loader, Thread.currentThread()));
    }

    public TraceModel getModel() {
        tree.trim();
        return new TraceModel(tree.getRoot(), tree.getNodeCount());
    }
}
