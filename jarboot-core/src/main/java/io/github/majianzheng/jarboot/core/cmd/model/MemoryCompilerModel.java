package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.Collection;
import java.util.List;

/**
 * @author majianzheng
 */
public class MemoryCompilerModel extends ResultModel {

    private List<String> files;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    public MemoryCompilerModel() {
    }

    public MemoryCompilerModel(List<String> files) {
        this.files = files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public MemoryCompilerModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public MemoryCompilerModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    @Override
    public String getName() {
        return "mc";
    }

}
