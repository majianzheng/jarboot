package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.cmd.Command;

/**
 * Store the common and fluently field.
 * @author jianzhengma
 */
public class EnvironmentContext {
    private static String server;
    private static String host;
    private static volatile Command currentCommand = null; //NOSONAR
    private EnvironmentContext() {}

    public static String getServer() {
        return server;
    }

    public static void setServer(String server) {
        EnvironmentContext.server = server;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        EnvironmentContext.host = host;
    }

    public static Command getCurrentCommand() {
        return currentCommand;
    }

    public static void setCurrentCommand(Command currentCommand) {
        EnvironmentContext.currentCommand = currentCommand;
    }
}
