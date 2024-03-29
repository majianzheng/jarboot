package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.Collection;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ClassSetVO implements Countable {
    private ClassLoaderVO classloader;
    private Collection<String> classes;
    private int segment;

    public ClassSetVO(ClassLoaderVO classloader, Collection<String> classes) {
        this(classloader, classes, 0);
    }

    public ClassSetVO(ClassLoaderVO classloader, Collection<String> classes, int segment) {
        this.classloader = classloader;
        this.classes = classes;
        this.segment = segment;
    }

    public ClassLoaderVO getClassloader() {
        return classloader;
    }

    public void setClassloader(ClassLoaderVO classloader) {
        this.classloader = classloader;
    }

    public Collection<String> getClasses() {
        return classes;
    }

    public void setClasses(Collection<String> classes) {
        this.classes = classes;
    }

    public int getSegment() {
        return segment;
    }

    public void setSegment(int segment) {
        this.segment = segment;
    }

    @Override
    public int size() {
        return classes != null ? classes.size() : 1;
    }
}
