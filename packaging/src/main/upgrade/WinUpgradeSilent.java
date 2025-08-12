import java.util.Arrays;
import java.util.List;
import java.io.File;

public class WinUpgradeSilent {
    public static void main(String[] args) {
        String winScriptBin = args[0];
        String installDir = args[1];
        System.out.println("winScriptBin: " + winScriptBin);
        System.out.println("installDir: " + installDir);
        File workDir = new File(winScriptBin);
        if (!workDir.exists()) {
            System.out.println("升级脚本目录不存在！");
            return;
        }
        List<String> cmd = Arrays.asList("upgrade.bat", "-d", installDir, "-y");
        System.out.println("开始执行升级脚本...");
        try {
            new ProcessBuilder(cmd).directory(workDir).start().waitFor();
            System.out.println("升级脚本执行完成，等待系统启动");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}