package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * sh
 * @author majianzheng
 */
@Name("sh")
@Summary("Execute shell")
@Description("Example:\n sh xxx.sh\n sh xxx.bat\n sh echo Hello\n sh ls -a")
public class ShCommandProcessor implements CommandProcessor {
    private static final boolean IS_WINDOWS;
    private Process process;
    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        IS_WINDOWS = os.startsWith("windows");
    }

    @Override
    public String process(CommandSession session, String[] args) {
        if (null != process) {
            session.end(false, "shell process is running!");
            return "";
        }
        if (null != args && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            if (IS_WINDOWS) {
                sb.append("cmd /c ");
            }
            for (String s : args) {
                sb.append(s).append(' ');
            }
            File dir = new File(new File("").getAbsolutePath());
            try {
                process = Runtime.getRuntime().exec(sb.toString(), null, dir);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                    String line;
                    while ((line = reader.readLine()) != null) {
                        session.console(line);
                    }
                    process.waitFor();
                }
            } catch (InterruptedException e) {
                session.end(false, e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
                session.end(false, e.getMessage());
            } finally {
                this.process = null;
            }
        } else {
            session.end(false, "args is empty!");
        }
        return "";
    }

    @Override
    public void onCancel() {
        if (null != this.process) {
            this.process.destroyForcibly();
        }
    }
}
