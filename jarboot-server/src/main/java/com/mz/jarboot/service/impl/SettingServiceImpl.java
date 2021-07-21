package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.OSUtils;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.constant.SettingPropConst;
import com.mz.jarboot.dto.GlobalSettingDTO;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.service.SettingService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class SettingServiceImpl implements SettingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public ServerSettingDTO getServerSetting(String server) {
        return PropertyFileUtils.getServerSetting(server);
    }

    @Override
    public void submitServerSetting(String server, ServerSettingDTO setting) {
        File file = getConfAndCheck(server, setting.getJar());
        Properties prop = PropertyFileUtils.getProperties(file);
        String jar = setting.getJar();
        if (null == jar) {
            jar = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.JAR, jar);
        String vm = setting.getVm();
        if (null == vm) {
            vm = SettingPropConst.DEFAULT_VM_FILE;
        }
        prop.setProperty(SettingPropConst.VM, vm);
        String args = setting.getArgs();
        if (null == args) {
            args = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.ARGS, args);
        if (null == setting.getPriority()) {
            prop.setProperty(SettingPropConst.PRIORITY, StringUtils.EMPTY);
        } else {
            prop.setProperty(SettingPropConst.PRIORITY, setting.getPriority().toString());
        }
        checkAndSetWorkHome(setting, prop);
        checkAndSetJavaHome(setting, prop);
        checkAndSetEnv(setting, prop);
        if (null == setting.getDaemon()) {
            prop.setProperty(SettingPropConst.DAEMON, SettingPropConst.VALUE_TRUE);
        } else {
            prop.setProperty(SettingPropConst.DAEMON, setting.getDaemon().toString());
        }
        if (null == setting.getJarUpdateWatch()) {
            prop.setProperty(SettingPropConst.JAR_UPDATE_WATCH, SettingPropConst.VALUE_TRUE);
        } else {
            prop.setProperty(SettingPropConst.JAR_UPDATE_WATCH, setting.getJarUpdateWatch().toString());
        }
        PropertyFileUtils.storeProperties(file, prop);
    }

    private void checkAndSetWorkHome(ServerSettingDTO setting, Properties prop) {
        String workDirectory = setting.getWorkDirectory();
        if (StringUtils.isNotEmpty(workDirectory)) {
            checkDirExist(workDirectory);
        } else {
            workDirectory = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.WORK_DIR, workDirectory);
    }

    private void checkAndSetJavaHome(ServerSettingDTO setting, Properties prop) {
        String jdkPath = setting.getJdkPath();
        if (StringUtils.isNotEmpty(jdkPath)) {
            String javaFile = jdkPath + File.separator + CommonConst.BIN_NAME +
                    File.separator + CommonConst.JAVA_CMD;
            if (OSUtils.isWindows()) {
                javaFile += CommonConst.EXE_EXT;
            }
            checkFileExist(javaFile);
        } else {
            jdkPath = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.JDK_PATH, jdkPath);
    }

    private void checkAndSetEnv(ServerSettingDTO setting, Properties prop) {
        String env = setting.getEnv();
        if (PropertyFileUtils.checkEnvironmentVar(env)) {
            if (null == env) {
                env = StringUtils.EMPTY;
            }
            prop.setProperty(SettingPropConst.ENV, env);
        } else {
            throw new MzException(ResultCodeConst.VALIDATE_FAILED,
                    String.format("环境变量配置错误(%s)！", setting.getEnv()));
        }
    }

    @Override
    public GlobalSettingDTO getGlobalSetting() {
        return SettingUtils.getGlobalSetting();
    }

    @Override
    public void submitGlobalSetting(GlobalSettingDTO setting) {
        SettingUtils.updateGlobalSetting(setting);
    }

    @Override
    public String getVmOptions(String server, String file) {
        Path path = Paths.get(file);
        if (!path.isAbsolute()) {
            path = Paths.get(SettingUtils.getServerPath(server), file);
        }
        File f = path.toFile();
        String content = StringUtils.EMPTY;
        if (f.exists()) {
            try {
                content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MzException("Read file error.", e);
            }
        }
        return content;
    }

    @Override
    public void saveVmOptions(String server, String file, String content) {
        Path path = Paths.get(file);
        if (!path.isAbsolute()) {
            path = Paths.get(SettingUtils.getServerPath(server), file);
        }
        File f = path.toFile();
        if (!f.exists()) {
            try {
                if (!f.createNewFile()) {
                    throw new MzException("Create file failed.");
                }
            } catch (IOException e) {
                throw new MzException("Create file error.", e);
            }
        }
        try {
            FileUtils.writeStringToFile(f, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MzException("Write file error.", e);
        }
    }

    private File getConfAndCheck(String server, String jar) {
        File file = SettingUtils.getServerSettingFile(server);
        if (!file.exists()) {
            try {
                boolean rlt = file.createNewFile();
                if (!rlt) {
                    logger.debug("Config file({}) create failed.", file.getPath());
                }
            } catch (IOException e) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
        if (StringUtils.isNotEmpty(jar)) {
            Path path = Paths.get(jar);
            File jarFile = path.isAbsolute() ? path.toFile() :
                    FileUtils.getFile(SettingUtils.getServerPath(server), jar);
            if (!jarFile.exists() || !jarFile.isFile()) {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("jar文件(%s)不存在！", jar));
            }
        }
        return file;
    }

    private void checkDirExist(String path) {
        File dir = FileUtils.getFile(path);
        if (dir.exists() && dir.isDirectory()) {
            return;
        }
        throw new MzException(ResultCodeConst.NOT_EXIST, path + "不存在");
    }

    private void checkFileExist(String file) {
        File dir = FileUtils.getFile(file);
        if (dir.exists() && dir.isFile()) {
            return;
        }
        throw new MzException(ResultCodeConst.NOT_EXIST, file + "不存在");
    }
}
