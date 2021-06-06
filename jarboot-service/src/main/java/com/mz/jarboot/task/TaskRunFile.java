package com.mz.jarboot.task;

import com.mz.jarboot.constant.CommonConst;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class TaskRunFile implements Serializable {
    private static final long serialVersionUID = 1204122041950251201L;
    private final Map<String, TaskRunInfo> taskRunMap = new HashMap<>();
    public void setTaskInfo(String name, String status, Integer pid) {
        TaskRunInfo taskRunInfo = taskRunMap.getOrDefault(name, null);
        if (null == taskRunInfo) {
            taskRunInfo = new TaskRunInfo();
            taskRunMap.put(name, taskRunInfo);
        }
        if (null != pid) {
            taskRunInfo.setPid(pid);
        }
        switch (status) {
            case CommonConst.STATUS_RUNNING:
                taskRunInfo.setStartedTime(new Date());
                break;
            case CommonConst.STATUS_STARTING:
            case CommonConst.STATUS_STOPPING:
                taskRunInfo.setActionTime(new Date());
                break;
            default:
                break;
        }
        if (StringUtils.isNotEmpty(status)) {
            taskRunInfo.setStatus(status);
        }
    }
    public boolean hasNotFinished() {
        if (taskRunMap.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, TaskRunInfo> entry : this.taskRunMap.entrySet()) {
            String status = entry.getValue().getStatus();
            Date actionTime = entry.getValue().getActionTime();
            long actionTimeStamp = 0L;
            if (null != actionTime) {
                actionTimeStamp = actionTime.getTime();
            }
            if (StringUtils.equals(CommonConst.STATUS_STARTING, status) ||
                    StringUtils.equals(CommonConst.STATUS_STOPPING, status)) {
                return ((System.currentTimeMillis() - actionTimeStamp) < 30000);
            }
        }
        return false;
    }
    public String getTaskStatus(String name) {
        TaskRunInfo taskRunInfo = taskRunMap.get(name);
        if (null != taskRunInfo) {
            return taskRunInfo.getStatus();
        }
        return CommonConst.STATUS_STOPPED;
    }
    public Date getTaskStartTime(String name) {
        TaskRunInfo taskRunInfo = taskRunMap.get(name);
        if (null != taskRunInfo) {
            return taskRunInfo.getStartedTime();
        }
        return null;
    }
    public Integer getTaskPid(String name) {
        TaskRunInfo taskRunInfo = taskRunMap.get(name);
        if (null != taskRunInfo) {
            return taskRunInfo.getPid();
        }
        return -1;
    }
    public Date getActionTime(String name) {
        TaskRunInfo taskRunInfo = taskRunMap.get(name);
        if (null != taskRunInfo) {
            return taskRunInfo.getActionTime();
        }
        return null;
    }
    private static class TaskRunInfo implements Serializable {
        private static final long serialVersionUID = 1203000049710251201L;
        private String status;
        private Date startedTime;
        private Integer pid;
        private Date actionTime;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getStartedTime() {
            return startedTime;
        }

        public void setStartedTime(Date startedTime) {
            this.startedTime = startedTime;
        }

        public Integer getPid() {
            return pid;
        }

        public void setPid(Integer pid) {
            this.pid = pid;
        }

        public Date getActionTime() {
            return actionTime;
        }

        public void setActionTime(Date actionTime) {
            this.actionTime = actionTime;
        }
    }
}
