package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.pojo.UpgradeProgress;
import io.github.majianzheng.jarboot.common.utils.*;
import io.github.majianzheng.jarboot.service.UpgradeStoreFileCallback;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.service.UpgradeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 升级服务
 * @author majianzheng
 */
@Slf4j
@Service
public class UpgradeServiceImpl implements UpgradeService {
    private final AtomicBoolean upgrading = new AtomicBoolean(false);
    @Resource
    private ServerRuntimeService serverRuntimeService;

    @Override
    public boolean isUpgrading() {
        return upgrading.get();
    }

    @Override
    public void upgrade(String url) {
        if (!upgrading.compareAndSet(false, true)) {
            throw new JarbootException("正在升级中，请稍后...");
        }
        File instFile = installPackage();
        if (instFile.exists()) {
            FileUtils.deleteQuietly(instFile);
        }
        updateProgress("正在下载安装包...", CommonConst.STEP_FILE_INIT);
        try (OutputStream os = FileUtils.newOutputStream(instFile, false)) {
            HttpUtils.get(url, os, null);
            updateProgress("安装包下载完成", CommonConst.STEP_FILE_INIT);
        } catch (Exception e) {
            updateProgress(e.getMessage(), CommonConst.STEP_UPGRADE_FAILED);
            upgrading.set(false);
            throw new JarbootException(e);
        }
        doUpgrade(instFile);
    }

    @Override
    public void upgrade(String file, InputStream is, UpgradeStoreFileCallback callback) {
        if (!upgrading.compareAndSet(false, true)) {
            throw new JarbootException("正在升级中，请稍后...");
        }
        File instFile = installPackage();
        if (instFile.exists()) {
            FileUtils.deleteQuietly(instFile);
        }
        try {
            FileUtils.copyInputStreamToFile(is, instFile);
            if (callback != null) {
                callback.run(instFile);
            }
        } catch (Exception e) {
            upgrading.set(false);
            throw new JarbootException(e);
        }
        doUpgrade(instFile);
    }

    private void checkEnv() {
        ServerRuntimeInfo info = serverRuntimeService.getServerRuntimeInfo();
        if (Boolean.TRUE.equals(info.getInDocker())) {
            throw new JarbootException("当前服务运行在docker中，请使用docker compose升级");
        }
    }

    private void checkAllShutdown() {
        if (!AgentManager.getInstance().isAllShutdown()) {
            updateProgress("请先停止所有服务", CommonConst.STEP_UPGRADE_FAILED);
            throw new JarbootException("请先停止所有服务");
        }
    }

    private void doUpgrade(File installPackage) {
        File dir = getUploadCacheDir();
        try {
            updateProgress("开始执行升级...", CommonConst.STEP_FILE_INIT);
            checkAllShutdown();
            checkEnv();
            File distDir = FileUtils.getFile(dir, "jarboot");
            if (distDir.exists()) {
                FileUtils.deleteQuietly(distDir);
            }
            ZipUtils.unZip(installPackage, dir);
            updateProgress("开始检验安装包...", CommonConst.STEP_FILE_CHECK);
            updateProgress("检验脚本工具...", CommonConst.STEP_FILE_CHECK);
            checkBinDir(distDir);
            updateProgress("检验脚本工具完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("校验组件库...", CommonConst.STEP_FILE_CHECK);
            checkComponent(distDir);
            updateProgress("校验组件库完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("校验系统配置...", CommonConst.STEP_FILE_CHECK);
            checkDir(FileUtils.getFile(distDir, "conf"));
            updateProgress("校验系统配置完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("校验资源文件...", CommonConst.STEP_FILE_CHECK);
            checkDir(FileUtils.getFile(distDir, "fe"));
            updateProgress("校验资源文件完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("检验插件库...", CommonConst.STEP_FILE_CHECK);
            checkDir(FileUtils.getFile(distDir, "plugins"));
            updateProgress("检验插件库完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("校验工作空间...", CommonConst.STEP_FILE_CHECK);
            checkDir(FileUtils.getFile(distDir, "workspace"));
            updateProgress("校验工作空间完成。", CommonConst.STEP_FILE_CHECK);
            updateProgress("开始升级...", CommonConst.STEP_START_UPGRADE);
            backgroundStart(distDir);
        } catch (Exception e) {
            upgrading.set(false);
            log.error("升级失败: {}", e.getMessage(), e);
            updateProgress(e.getMessage(), CommonConst.STEP_UPGRADE_FAILED);
        }
    }

    private void backgroundStart(File distDir) {
        TaskUtils.getTaskExecutor().schedule(() -> {
            try {
                startUpgradeScript(distDir);
            } catch (Exception e) {
                upgrading.compareAndSet(true, false);
                log.error("升级失败", e);
                updateProgress("升级失败" + e.getMessage(), CommonConst.STEP_UPGRADE_FAILED);
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void startUpgradeScript(File distDir) throws IOException {
        String home = SettingUtils.getHomePath();
        File workDir;
        List<String> cmd;
        if (OSUtils.isWindows()) {
            workDir = FileUtils.getFile(distDir, "bin", "windows");
            String jdkPath = SettingUtils.getJdkPath();
            String javaw = FileUtils.getFile(jdkPath, "bin", "javaw.exe").getAbsolutePath();
            cmd = Arrays.asList(javaw, "WinUpgradeSilent", workDir.getAbsolutePath(), home);
        } else {
            workDir = FileUtils.getFile(distDir, "bin");
            String command = String.format(
                    "nohup bash upgrade.sh -d '%s' -y >\"%s/logs/upgrade.log\" 2>&1 &",
                    home.replace("'", "'\"'\"'"), // 转义单引号
                    home
            );
            cmd = Arrays.asList("bash", "-c", command);
        }
        updateProgress("即将开始执行升级脚本，系统将会关闭！", CommonConst.STEP_START_UPGRADE);
        new ProcessBuilder(cmd).directory(workDir).start();
        updateProgress("正在执行升级脚本，系统即将关闭，请稍后...", CommonConst.STEP_START_UPGRADE);
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
            String[] scriptFiles = new String[]{"startup.cmd", "shutdown.cmd", "shutdown-all.cmd", "status.cmd", "upgrade.bat", "WinUpgradeSilent.class"};
            for (String scriptFile : scriptFiles) {
                checkFile(FileUtils.getFile(binDir, "windows", scriptFile));
            }
        } else {
            String[] scriptFiles = new String[]{"startup.sh", "shutdown.sh", "shutdown-all.sh", "status.sh", "upgrade.sh"};
            for (String scriptFile : scriptFiles) {
                checkFile(FileUtils.getFile(binDir, scriptFile));
            }
        }
    }

    private void checkComponent(File distDir) {
        File componentDir = FileUtils.getFile(distDir, CommonConst.COMPONENTS_NAME);
        checkDir(componentDir);
        checkFile(FileUtils.getFile(componentDir, "jarboot-server.jar"));
        checkFile(FileUtils.getFile(componentDir, "jarboot-agent.jar"));
        checkFile(FileUtils.getFile(componentDir, "jarboot-spy.jar"));
        final String[] parseFiles = new String[]{"jarboot-core.jar", "jarboot-tools.jar"};
        Set<String> dependencies = new HashSet<>(16);
        for (String fileName : parseFiles) {
            File file = FileUtils.getFile(SettingUtils.getHomePath(), CommonConst.COMPONENTS_NAME, fileName);
            checkFile(file);
            Set<String> paths = CommonUtils.getJarDependencies(file);
            if (!CollectionUtils.isEmpty(paths)) {
                dependencies.addAll(paths);
            }
        }
        for (String path : dependencies) {
            if (path.endsWith(CommonConst.JAR_EXT)) {
                File libFile = FileUtils.getFile(SettingUtils.getHomePath(), CommonConst.COMPONENTS_NAME, path);
                checkFile(libFile);
            }
        }
    }

    private void checkFile(File file) {
        if (!file.exists()) {
            throw new JarbootException("安装包缺少" + file.getName());
        }
        if (!file.isFile()) {
            throw new JarbootException(file.getName() + "不是文件");
        }
    }

    private void checkDir(File file) {
        if (!file.exists()) {
            throw new JarbootException("安装包缺少" + file.getName());
        }
        if (!file.isDirectory()) {
            throw new JarbootException(file.getName() + "不是文件夹");
        }
    }

    @Override
    public void updateProgress(String msg, int action) {
        UpgradeProgress progress = new UpgradeProgress();
        progress.setAction(action);
        progress.setMsg(msg);
        progress.setHost(ClusterClientManager.getInstance().getSelfHost());
        MessageUtils.globalEvent(JsonUtils.toJsonString(progress), FrontEndNotifyEventType.UPGRADE_PROGRESS);
    }

    private File getUploadCacheDir() {
        return FileUtils.getFile(CacheDirHelper.getCacheDir(), "upgrade");
    }
    private File installPackage() {
        return FileUtils.getFile(getUploadCacheDir(), "jarboot.zip");
    }
}
