package io.github.majianzheng.jarboot.core.cmd.model;

import io.github.majianzheng.jarboot.core.GlobalOptions;
import io.github.majianzheng.jarboot.core.utils.affect.EnhancerAffect;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class EnhancerAffectVO {

    private final long cost;
    private final int methodCount;
    private final int classCount;
    private final long listenerId;
    private Throwable throwable;
    private List<String> classDumpFiles;
    private List<String> methods;

    public EnhancerAffectVO(EnhancerAffect affect) {
        this.cost = affect.cost();
        this.classCount = affect.cCnt();
        this.methodCount = affect.mCnt();
        this.listenerId = affect.getListenerId();
        this.throwable = affect.getThrowable();

        if (GlobalOptions.isDump) {
            classDumpFiles = new ArrayList<>();
            for (File classDumpFile : affect.getClassDumpFiles()) {
                classDumpFiles.add(classDumpFile.getAbsolutePath());
            }
        }

        if (GlobalOptions.verbose) {
            methods = new ArrayList<>();
            methods.addAll(affect.getMethods());
        }
    }

    public EnhancerAffectVO(long cost, int methodCount, int classCount, long listenerId) {
        this.cost = cost;
        this.methodCount = methodCount;
        this.classCount = classCount;
        this.listenerId = listenerId;
    }

    public long getCost() {
        return cost;
    }

    public int getClassCount() {
        return classCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public long getListenerId() {
        return listenerId;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public List<String> getClassDumpFiles() {
        return classDumpFiles;
    }

    public void setClassDumpFiles(List<String> classDumpFiles) {
        this.classDumpFiles = classDumpFiles;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }
}
