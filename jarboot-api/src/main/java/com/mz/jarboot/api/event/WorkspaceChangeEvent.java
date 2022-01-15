package com.mz.jarboot.api.event;

/**
 * 工作空间变动事件
 * @author jianzhengma
 */
public class WorkspaceChangeEvent implements JarbootEvent {
    private final String workspace;
    private final String oldWorkspace;

    public WorkspaceChangeEvent(String workspace, String oldWorkspace) {
        this.workspace = workspace;
        this.oldWorkspace = oldWorkspace;
    }

    public String getWorkspace() {
        return this.workspace;
    }

    public String getOldWorkspace() {
        return this.oldWorkspace;
    }
}
