package com.mz.jarboot.api.event;

/**
 * 工作空间变动事件
 * @author jianzhengma
 */
public class WorkspaceChangeEvent implements JarbootEvent {
    private String workspace;
    private String oldWorkspace;

    public WorkspaceChangeEvent() {

    }

    public WorkspaceChangeEvent(String workspace, String oldWorkspace) {
        this.workspace = workspace;
        this.oldWorkspace = oldWorkspace;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getOldWorkspace() {
        return oldWorkspace;
    }

    public void setOldWorkspace(String oldWorkspace) {
        this.oldWorkspace = oldWorkspace;
    }

    @Override
    public String toString() {
        return "WorkspaceChangeEvent{" +
                "workspace='" + workspace + '\'' +
                ", oldWorkspace='" + oldWorkspace + '\'' +
                '}';
    }
}
