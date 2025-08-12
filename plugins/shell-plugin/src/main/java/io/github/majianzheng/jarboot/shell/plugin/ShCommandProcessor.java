package io.github.majianzheng.jarboot.shell.plugin;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

/**
 * sh
 * @author majianzheng
 */
@Name("sh")
@Summary("Execute shell")
@Description("Example:\n sh xxx.sh\n sh xxx.bat\n sh echo Hello\n sh ls -a")
@SuppressWarnings({"java:S1874"})
public class ShCommandProcessor implements CommandProcessor {
    private static final boolean IS_WINDOWS;
    private Process process;
    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        IS_WINDOWS = os.startsWith("windows");
    }

    @SuppressWarnings({"squid:S106", "squid:S1148"})
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
            File dir = new File(UserDirHelper.getCurrentDir());
            try {
                process = Runtime.getRuntime().exec(sb.toString(), null, dir);
                InputStream inputStream = process.getInputStream();
                int b = -1;
                while (-1 != (b = inputStream.read())) {
                    //最终会使用jarboot-core中的StdConsoleOutputStream来实现
                    System.out.write(b);
                }
                process.waitFor();
            } catch (InterruptedException e) {
                session.end(false, e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
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
