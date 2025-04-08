package io.github.majianzheng.jarboot.common.utils;

import io.github.majianzheng.jarboot.common.AnsiLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 进程工具类
 * @author majianzheng
 */
@SuppressWarnings("all")
public class ProcessUtils {

    public static List<String> getChildPIDs(long parentPID) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux") || os.contains("mac")) {
            return getUnixChildPIDs(parentPID);
        } else if (os.contains("win")) {
            return getWindowsChildPIDs(parentPID);
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }

    // Linux/macOS: 使用 `pgrep` 命令获取子进程
    private static List<String> getUnixChildPIDs(long parentPID) throws IOException {
        List<String> childPIDs = new ArrayList<>();
        Process process = new ProcessBuilder("pgrep", "-P", String.valueOf(parentPID)).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                childPIDs.add(line.trim());
            }
        }
        return childPIDs;
    }

    // Windows: 使用 `wmic` 命令获取子进程
    private static List<String> getWindowsChildPIDs(long parentPID) throws IOException {
        List<String> childPIDs = new ArrayList<>();
        String command = "wmic process where (ParentProcessId=" + parentPID + ") get ProcessId";
        Process process = Runtime.getRuntime().exec(command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    childPIDs.add(line.trim());
                }
            }
        }
        return childPIDs;
    }

    public static void killByPid(String pid) {
        if (pid.isEmpty()) {
            return;
        }
        List<String> command = OSUtils.isWindows() ? Arrays.asList("taskkill", "/F", "/pid", pid) : Arrays.asList("kill", "-9", pid);
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }
}
