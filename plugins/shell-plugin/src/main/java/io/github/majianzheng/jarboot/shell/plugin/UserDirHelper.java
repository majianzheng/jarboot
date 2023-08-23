package io.github.majianzheng.jarboot.shell.plugin;

import java.io.File;

/**
 * @author majianzheng
 */
public class UserDirHelper {
    private static final String USER_DIR_KEY = "user.dir";
    public static String getCurrentDir() {
        String userDir = System.getProperty(USER_DIR_KEY);
        if (new File(userDir).exists()) {
            return userDir;
        }
        String path = new File("").getAbsolutePath();
        System.setProperty(USER_DIR_KEY, path);
        return path;
    }

    public static void setCurrentDir(String path) {
        System.setProperty(USER_DIR_KEY, path);
    }

    private UserDirHelper() {}
}
