package com.mz.jarboot.core.cmd.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Model of `heapdump` command
 * @author majianzheng
 */
public class HeapDumpModel extends ResultModel {

    private String dumpFile;
    private String encodedName;

    private boolean live;

    public HeapDumpModel() {

    }

    public HeapDumpModel(String dumpFile, boolean live) {
        this.dumpFile = dumpFile;
        this.encodedName = Base64.getEncoder().encodeToString(dumpFile.getBytes(StandardCharsets.UTF_8));
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

    public String getEncodedName() {
        return encodedName;
    }

    public void setEncodedName(String encodedName) {
        this.encodedName = encodedName;
    }

    @Override
    public String getName() {
        return "heapdump";
    }

}
