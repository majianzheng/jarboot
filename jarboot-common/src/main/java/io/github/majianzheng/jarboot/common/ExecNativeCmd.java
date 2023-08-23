package io.github.majianzheng.jarboot.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 以下代码，有一部分摘自开源项目Arthas
 * @author majianzheng
 */
public class ExecNativeCmd {
    private ExecNativeCmd() {
    }

    /**
     * Executes a command on the native command line and returns the result.
     *
     * @param cmdToRun
     *            Command to run
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> exec(String cmdToRun) {
        String[] cmd = cmdToRun.split(" ");
        return exec(cmd);
    }

    /**
     * Executes a command on the native command line and returns the result line by
     * line.
     *
     * @param cmdToRunWithArgs
     *            Command to run and args, in an array
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> exec(String[] cmdToRunWithArgs) {
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmdToRunWithArgs);
        } catch (SecurityException | IOException e) {
            return new ArrayList<>(0);
        }
        ArrayList<String> sa = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))){
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            p.waitFor();
        } catch (IOException e) {
            return new ArrayList<>(0);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return sa;
    }

    /**
     * Return first line of response for selected command.
     *
     * @param cmd2launch
     *            String command to be launched
     * @return String or empty string if command failed
     */
    public static String getFirstAnswer(String cmd2launch) {
        return getAnswerAt(cmd2launch, 0);
    }

    /**
     * Return response on selected line index (0-based) after running selected
     * command.
     *
     * @param cmd2launch
     *            String command to be launched
     * @param answerIdx
     *            int index of line in response of the command
     * @return String whole line in response or empty string if invalid index or
     *         running of command fails
     */
    public static String getAnswerAt(String cmd2launch, int answerIdx) {
        List<String> sa = ExecNativeCmd.exec(cmd2launch);

        if (answerIdx >= 0 && answerIdx < sa.size()) {
            return sa.get(answerIdx);
        }
        return "";
    }
}
