package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.Collection;

/**
 * Data model of OgnlCommand
 * @author majianzheng
 */
public class OgnlModel extends ResultModel {
    private Object value;
    private int expand = 1;

    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;


    @Override
    public String getName() {
        return "ognl";
    }

    public Object getValue() {
        return value;
    }

    public OgnlModel setValue(Object value) {
        this.value = value;
        return this;
    }

    public int getExpand() {
        return expand;
    }

    public OgnlModel setExpand(int expand) {
        this.expand = expand;
        return this;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public OgnlModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public OgnlModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
