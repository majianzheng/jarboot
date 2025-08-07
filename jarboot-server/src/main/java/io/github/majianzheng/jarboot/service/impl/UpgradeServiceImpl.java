package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.pojo.UpgradeProgress;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.ZipUtils;
import io.github.majianzheng.jarboot.service.UpgradeService;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 升级服务
 * @author majianzheng
 */
@Slf4j
@Service
public class UpgradeServiceImpl implements UpgradeService {
    // 0 文件解压缩 1 文件检验 2 开始升级 -1 升级失败
    private static final int STEP_FILE_INIT = 0;
    private static final int STEP_FILE_CHECK = 1;
    private static final int STEP_START_UPGRADE = 2;
    private static final int STEP_UPGRADE_FAILED = -1;
    @Override
    public void upgrade(String url) {
        checkAllShutdown();
    }

    @Override
    public void upgrade(String file, InputStream is) {
        File instFile = installPackage();
        if (instFile.exists()) {
            FileUtils.deleteQuietly(instFile);
        }
        try {
            FileUtils.copyInputStreamToFile(is, instFile);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
        doUpgrade(instFile);
    }

    private void checkAllShutdown() {
        if (!AgentManager.getInstance().isAllShutdown()) {
            updateProgress("请先停止所有服务", STEP_UPGRADE_FAILED);
            throw new JarbootException("请先停止所有服务");
        }
    }

    private void doUpgrade(File installPackage) {
        File dir = getUploadCacheDir();
        updateProgress("开始执行升级...", STEP_FILE_INIT);
        checkAllShutdown();
        try {
            File distDir = FileUtils.getFile(dir, "jarboot");
            if (distDir.exists()) {
                FileUtils.deleteQuietly(distDir);
            }
            ZipUtils.unZip(installPackage, dir);
            updateProgress("开始检验安装包...", STEP_FILE_CHECK);
            checkBinDir(distDir);

            updateProgress("开始升级...", STEP_START_UPGRADE);
            startUpgradeScript(distDir);
        } catch (Exception e) {
            log.error("升级失败: {}", e.getMessage(), e);
            updateProgress(e.getMessage(), STEP_UPGRADE_FAILED);
        }
    }

    private void startUpgradeScript(File distDir) throws Exception {
        String home = SettingUtils.getHomePath();
        File workDir;
        List<String> cmd;
        if (OSUtils.isWindows()) {
            workDir = FileUtils.getFile(distDir, "bin", "windows");
            cmd = Arrays.asList("cmd", "/c", "start", "/min", "upgrade.bat", "-d", home, "-y");
        } else {
            workDir = FileUtils.getFile(distDir, "bin");
            String command = String.format(
                    "nohup bash upgrade.sh -d '%s' -y >/dev/null 2>&1 &",
                    home.replace("'", "'\"'\"'") // 转义单引号
            );
            cmd = Arrays.asList("bash", "-c", command);
        }
        updateProgress("开始执行升级脚本，系统将会关闭！", STEP_START_UPGRADE);
        new ProcessBuilder(cmd).directory(workDir).start();
        updateProgress("正在执行升级脚本，系统将关闭，请稍后...", STEP_START_UPGRADE);
    }

    private void checkBinDir(File distDir) {
        File binDir = FileUtils.getFile(distDir, "bin");
        if (!binDir.exists()) {
            throw new JarbootException("安装包缺少bin目录");
        }
        if (!binDir.isDirectory()) {
            throw new JarbootException("bin目录不是目录");
        }
        if (OSUtils.isWindows()) {
            String[] scriptFiles = new String[]{"startup.cmd", "shutdown.cmd", "shutdown-all.cmd", "status.cmd", "upgrade.bat"};
            for (String scriptFile : scriptFiles) {
                File script = FileUtils.getFile(binDir, "windows", scriptFile);
                if (!script.exists()) {
                    throw new JarbootException("安装包缺少" + scriptFile);
                }
                if (!script.isFile()) {
                    throw new JarbootException(scriptFile + "不是文件");
                }
            }
        } else {
            String[] scriptFiles = new String[]{"startup.sh", "shutdown.sh", "shutdown-all.sh", "status.sh", "upgrade.sh"};
            for (String scriptFile : scriptFiles) {
                File script = FileUtils.getFile(binDir, scriptFile);
                if (!script.exists()) {
                    throw new JarbootException("安装包缺少" + scriptFile);
                }
                if (!script.isFile()) {
                    throw new JarbootException(scriptFile + "不是文件");
                }
            }
        }
    }

    private void updateProgress(String msg, int action) {
        UpgradeProgress progress = new UpgradeProgress();
        progress.setAction(action);
        progress.setMsg(msg);
        MessageUtils.globalEvent(JsonUtils.toJsonString(progress), FrontEndNotifyEventType.UPGRADE_PROGRESS);
    }

    private File getUploadCacheDir() {
        return FileUtils.getFile(CacheDirHelper.getCacheDir(), "upgrade");
    }
    private File installPackage() {
        return FileUtils.getFile(getUploadCacheDir(), "jarboot.zip");
    }
}
