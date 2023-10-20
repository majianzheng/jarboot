package io.github.majianzheng.jarboot.core.cmd.model;

import io.github.majianzheng.jarboot.common.utils.AesUtils;

import java.io.File;

/**
 * Model of `heapdump` command
 * @author majianzheng
 */
public class HeapDumpModel extends ResultModel {

    private String dumpFile;
    private String encrypted;

    private boolean live;

    public HeapDumpModel() {

    }

    public HeapDumpModel(File file, boolean live) {
        this.dumpFile = file.getName();
        this.encrypted = AesUtils.encrypt(file.getAbsolutePath());
        this.live = live;
    }

    public String getDumpFile() {
        return dumpFile;
    }

    public void setDumpFile(String dumpFile) {
        this.dumpFile = dumpFile;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public String getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(String encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String getName() {
        return "heapdump";
    }

}
