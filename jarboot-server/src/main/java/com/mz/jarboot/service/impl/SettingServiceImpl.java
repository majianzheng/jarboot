package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.OSUtils;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
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
            jar = CommonConst.EMPTY_STRING;
        }
        prop.setProperty("jar", jar);
        String jvm = setting.getJvm();
        if (null == jvm) {
            jvm = "boot.vmoptions";
        }
        prop.setProperty("jvm", jvm);
        String args = setting.getArgs();
        if (null == args) {
            args = CommonConst.EMPTY_STRING;
        }
        prop.setProperty("args", args);
        if (null == setting.getPriority()) {
            prop.setProperty("priority", CommonConst.EMPTY_STRING);
        } else {
            prop.setProperty("priority", setting.getPriority().toString());
        }
        checkAndSetWorkHome(setting, prop);
        checkAndSetJavaHome(setting, prop);
        checkAndSetEnv(setting, prop);
        if (null == setting.getDaemon()) {
            prop.setProperty("daemon", "true");
        } else {
            prop.setProperty("daemon", setting.getDaemon().toString());
        }
        if (null == setting.getJarUpdateWatch()) {
            prop.setProperty("jarUpdateWatch", "true");
        } else {
            prop.setProperty("jarUpdateWatch", setting.getJarUpdateWatch().toString());
        }
        PropertyFileUtils.storeProperties(file, prop);
    }

    private void checkAndSetWorkHome(ServerSettingDTO setting, Properties prop) {
        String workHome = setting.getWorkHome();
        if (StringUtils.isNotEmpty(workHome)) {
            checkDirExist(workHome);
        } else {
            workHome = CommonConst.EMPTY_STRING;
        }
        prop.setProperty("workHome", workHome);
    }

    private void checkAndSetJavaHome(ServerSettingDTO setting, Properties prop) {
        String javaHome = setting.getJavaHome();
        if (StringUtils.isNotEmpty(javaHome)) {
            String javaFile = javaHome + File.separator + "bin" + File.separator + "java";
            if (OSUtils.isWindows()) {
                javaFile += ".exe";
            }
            checkFileExist(javaFile);
        } else {
            javaHome = CommonConst.EMPTY_STRING;
        }
        prop.setProperty("javaHome", javaHome);
    }

    private void checkAndSetEnv(ServerSettingDTO setting, Properties prop) {
        String envp = setting.getEnvp();
        if (PropertyFileUtils.checkEnvironmentVar(envp)) {
            if (null == envp) {
                envp = "";
            }
            prop.setProperty("envp", envp);
        } else {
            throw new MzException(ResultCodeConst.VALIDATE_FAILED,
                    String.format("环境变量配置错误(%s)！", setting.getEnvp()));
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
            String jarPath = SettingUtils.getServerPath(server) + File.separator + jar;
            File jarFile = new File(jarPath);
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
