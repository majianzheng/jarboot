package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.WsEventEnum;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * @author majianzheng
 */
@Service
public class SettingServiceImpl implements SettingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public ServerSetting getServiceSetting(String serviceName) {
        return PropertyFileUtils.getServerSetting(serviceName);
    }

    @Override
    public void submitServiceSetting(ServerSetting setting) {
        final String path = SettingUtils.getServerPath(setting.getName());
        File file = getConfAndCheck(path);
        Properties prop = PropertyFileUtils.getProperties(file);
        String command = setting.getCommand();
        if (null == command) {
            command = StringUtils.EMPTY;
        } else {
            command = command.replace('\n', ' ');
        }
        prop.setProperty(SettingPropConst.COMMAND, command);
        String vm = setting.getVm();
        if (null == vm) {
            vm = SettingPropConst.DEFAULT_VM_FILE;
        } else {
            checkFileExist(vm, path);
        }
        prop.setProperty(SettingPropConst.VM, vm);
        String args = setting.getArgs();
        if (null == args) {
            args = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.ARGS, args);
        String group = setting.getGroup();
        if (null == group) {
            group = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.GROUP, group);
        if (null == setting.getPriority()) {
            prop.setProperty(SettingPropConst.PRIORITY, StringUtils.EMPTY);
        } else {
            prop.setProperty(SettingPropConst.PRIORITY, setting.getPriority().toString());
        }
        checkAndSet(path, setting, prop);
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
        setting.setWorkspace(SettingUtils.getWorkspace());
        saveServerConfig(path, setting, file, prop);
    }

    private void saveServerConfig(String path, ServerSetting setting, File file, Properties prop) {
        //检查文件名是否修改
        ServerSetting preSetting = PropertyFileUtils.getServerSetting(setting.getName());
        if (!Objects.equals(setting.getName(), preSetting.getName())) {
            String sid = SettingUtils.createSid(path);
            //名字发生了变更，需要修改文件夹的名字，先检查是否正在运行
            if (AgentManager.getInstance().isOnline(sid)) {
                throw new JarbootRunException("服务正在运行，请先停止服务再重命名服务！");
            }
            PropertyFileUtils.storeProperties(file, prop);
            //开始重命名
            File dir = FileUtils.getFile(path);
            File renamed = FileUtils.getFile(SettingUtils.getWorkspace(), setting.getName());
            if (renamed.exists()) {
                throw new JarbootRunException(setting.getName() + "已经存在！");
            }
            if (!dir.renameTo(renamed)) {
                throw new JarbootRunException("重命名服务失败！");
            }
            //重命名成功，发布工作空间变化事件
            WebSocketManager.getInstance().createGlobalEvent(StringUtils.SPACE,
                    StringUtils.EMPTY, WsEventEnum.WORKSPACE_CHANGE);
        } else {
            PropertyFileUtils.storeProperties(file, prop);
        }
        //更新缓存配置，根据文件时间戳判定是否更新了
        PropertyFileUtils.getServerSetting(setting.getName());
    }

    private void checkAndSet(String path, ServerSetting setting, Properties prop) {
        String workDirectory = setting.getWorkDirectory();
        if (StringUtils.isNotEmpty(workDirectory)) {
            checkDirExist(workDirectory);
        } else {
            workDirectory = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.WORK_DIR, workDirectory);

        String jdkPath = setting.getJdkPath();
        if (StringUtils.isNotEmpty(jdkPath)) {
            String javaFile = jdkPath + File.separator + CommonConst.BIN_NAME +
                    File.separator + CommonConst.JAVA_CMD;
            if (OSUtils.isWindows()) {
                javaFile += CommonConst.EXE_EXT;
            }

            checkFileExist(javaFile, path);
        } else {
            jdkPath = StringUtils.EMPTY;
        }
        prop.setProperty(SettingPropConst.JDK_PATH, jdkPath);

        String env = setting.getEnv();
        if (PropertyFileUtils.checkEnvironmentVar(env)) {
            if (null == env) {
                env = StringUtils.EMPTY;
            }
            prop.setProperty(SettingPropConst.ENV, env);
        } else {
            throw new JarbootException(ResultCodeConst.VALIDATE_FAILED,
                    String.format("环境变量配置错误(%s)！", setting.getEnv()));
        }
    }

    @Override
    public GlobalSetting getGlobalSetting() {
        return SettingUtils.getGlobalSetting();
    }

    @Override
    public void submitGlobalSetting(GlobalSetting setting) {
        SettingUtils.updateGlobalSetting(setting);
    }

    @Override
    public String getVmOptions(String serviceName, String file) {
        Path path = SettingUtils.getPath(file);
        if (!path.isAbsolute()) {
            path = SettingUtils.getPath(SettingUtils.getServerPath(serviceName), file);
        }
        File f = path.toFile();
        String content = StringUtils.EMPTY;
        if (f.exists()) {
            try {
                content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new JarbootException("Read file error.", e);
            }
        }
        return content;
    }

    @Override
    public void saveVmOptions(String serviceName, String file, String content) {
        Path path = SettingUtils.getPath(file);
        if (!path.isAbsolute()) {
            path = SettingUtils.getPath(SettingUtils.getServerPath(serviceName), file);
        }
        File f = path.toFile();
        if (!f.exists()) {
            try {
                if (!f.createNewFile()) {
                    throw new JarbootException("Create file failed.");
                }
            } catch (IOException e) {
                throw new JarbootException("Create file error.", e);
            }
        }
        try {
            FileUtils.writeStringToFile(f, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JarbootException("Write file error.", e);
        }
    }

    private File getConfAndCheck(String p) {
        File file = SettingUtils.getServerSettingFile(p);
        if (!file.exists()) {
            try {
                boolean rlt = file.createNewFile();
                if (!rlt) {
                    logger.debug("Config file({}) create failed.", file.getPath());
                }
            } catch (IOException e) {
                throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
        return file;
    }

    private void checkDirExist(String path) {
        File dir = FileUtils.getFile(path);
        if (dir.exists() && dir.isDirectory()) {
            return;
        }
        throw new JarbootException(ResultCodeConst.NOT_EXIST, path + "不存在");
    }

    private void checkFileExist(String file, String serverPath) {
        File dir = SettingUtils.isAbsolutePath(file) ?
                FileUtils.getFile(file)
                :
                FileUtils.getFile(serverPath, file);
        if (dir.exists() && dir.isFile()) {
            return;
        }
        throw new JarbootException(ResultCodeConst.NOT_EXIST, file + "不存在");
    }
}
