import java.util.Arrays;
import java.util.List;
import java.nio.file.Paths;
import java.io.*;

public class WinUpgradeSilent {
    public static void main(String[] args) {
        String winScriptBin = args[0];
        String installDir = args[1];
        File logFile = Paths.get(installDir, "logs", "upgrade.log").toFile();
        PrintStream ps1 = null;
        try (OutputStream os = new FileOutputStream(logFile, false); PrintStream ps = new PrintStream(os)) {
            ps1 = ps;
            ps.println("winScriptBin: " + winScriptBin);
            ps.println("installDir: " + installDir);
            File workDir = new File(winScriptBin);
            if (!workDir.exists()) {
                ps.println("升级脚本目录不存在！");
                return;
            }
            List<String> cmd = Arrays.asList("upgrade.bat", "-d", installDir, "-y");
            ps.println("开始执行升级脚本...");
            Process process = new ProcessBuilder(cmd).directory(workDir).start();
            Thread thread1 = new Thread(() -> {
                // 读取所有输出
                try (InputStream in = process.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ps.println(line);
                    }
                } catch (Exception e) {
                    e.printStackTrace(ps);
                }
            });
            thread1.setName("upgrade-script-output");
            thread1.setDaemon(true);
            thread1.start();

            Thread thread2 = new Thread(() -> {
                // 读取错误输出
                try (InputStream errorStream = process.getErrorStream();
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        ps.println("ERROR: " + line);
                    }
                } catch (Exception e) {
                    e.printStackTrace(ps);
                }
            });
            thread2.setName("upgrade-script-err");
            thread2.setDaemon(true);
            thread2.start();

            ps.println("等待升级脚本执行完成...");
            int exitCode = process.waitFor();
            ps.println("升级脚本执行完成，退出码: " + exitCode);
            ps.println("升级脚本执行完成，等待系统启动");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace(null == ps1 ? System.out : ps1);
        }
    }
}