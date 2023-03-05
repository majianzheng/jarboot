package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.WorkspaceChangeEvent;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.SettingPropConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.notify.FrontEndNotifyEventType;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.utils.MessageUtils;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author majianzheng
 */
@Service
public class SettingServiceImpl implements SettingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private TaskRunCache taskRunCache;

    @Override
    public ServiceSetting getServiceSetting(String serviceName) {
        ServiceSetting setting =  PropertyFileUtils.getServiceSetting(serviceName);
        setting.setVmContent(this.getVmOptions(serviceName, setting.getVm()));
        return setting;
    }

    @Override
    public void submitServiceSetting(ServiceSetting setting) {
        String type = setting.getApplicationType();
        if (StringUtils.isBlank(type)) {
            throw new JarbootRunException("应用类型不可为空！");
        }
        if (StringUtils.isEmpty(setting.getName())) {
            throw new JarbootRunException("名称不可为空！");
        }
        if (!Arrays.asList(CommonConst.SHELL_TYPE, CommonConst.JAVA_CMD).contains(type)) {
            throw new JarbootRunException("应用类型仅支持java与shell！");
        }
        if (CommonConst.SHELL_TYPE.equals(type) && StringUtils.isEmpty(setting.getCommand())) {
            throw new JarbootRunException("启动命令不可为空！");
        }
        final String path = SettingUtils.getServicePath(setting.getName());
        String sid = SettingUtils.createSid(path);
        if (StringUtils.isNotEmpty(setting.getSid()) && !setting.getSid().equals(sid)) {
            // 发生了重命名，获取工作空间下所有服务
            renameService(setting, path);
            MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
        }
        File settingFile = SettingUtils.getServiceSettingFile(path);
        Properties properties = fillSettingProperties(setting, path, settingFile);
        setting.setWorkspace(SettingUtils.getWorkspace());
        saveSettingProperties(settingFile, properties);
        // 保存vmContent
        if (CommonConst.JAVA_CMD.equals(type)) {
            saveVmOptions(setting.getName(), setting.getVm(), setting.getVmContent());
        }
        //更新缓存配置，根据文件时间戳判定是否更新了
        PropertyFileUtils.getServiceSetting(setting.getName());
    }

    private Properties fillSettingProperties(ServiceSetting setting, String path, File settingFile) {
        Properties properties = settingFile.exists() ? PropertyFileUtils.getProperties(settingFile) : new Properties();
        String command = setting.getCommand();
        if (null == command) {
            command = StringUtils.EMPTY;
        } else {
            command = command.replace('\n', ' ');
        }
        properties.setProperty(SettingPropConst.COMMAND, command);
        String vm = setting.getVm();
        if (null == vm) {
            vm = SettingPropConst.DEFAULT_VM_FILE;
        } else {
            if (!SettingPropConst.DEFAULT_VM_FILE.equals(vm)) {
                checkFileExist(vm, path);
            }
        }
        properties.setProperty(SettingPropConst.APP_TYPE, setting.getApplicationType());
        properties.setProperty(SettingPropConst.VM, vm);
        String args = setting.getArgs();
        if (null == args) {
            args = StringUtils.EMPTY;
        }
        properties.setProperty(SettingPropConst.ARGS, args);
        String group = setting.getGroup();
        if (null == group) {
            group = StringUtils.EMPTY;
        }
        properties.setProperty(SettingPropConst.GROUP, group);
        if (null == setting.getPriority()) {
            properties.setProperty(SettingPropConst.PRIORITY, StringUtils.EMPTY);
        } else {
            properties.setProperty(SettingPropConst.PRIORITY, setting.getPriority().toString());
        }
        checkAndSet(path, setting, properties);
        if (null == setting.getDaemon()) {
            properties.setProperty(SettingPropConst.DAEMON, SettingPropConst.VALUE_TRUE);
        } else {
            properties.setProperty(SettingPropConst.DAEMON, setting.getDaemon().toString());
        }
        if (null == setting.getJarUpdateWatch()) {
            properties.setProperty(SettingPropConst.JAR_UPDATE_WATCH, SettingPropConst.VALUE_TRUE);
        } else {
            properties.setProperty(SettingPropConst.JAR_UPDATE_WATCH, setting.getJarUpdateWatch().toString());
        }
        return properties;
    }

    private void renameService(ServiceSetting setting, String path) {
        List<ServiceInstance> services = taskRunCache.getServiceList();
        ServiceInstance pre = services
                .stream()
                .filter(service -> setting.getSid().equals(service.getSid()))
                .findFirst().orElse(null);
        if (null != pre) {
            if (AgentManager.getInstance().isOnline(pre.getSid())) {
                throw new JarbootRunException("服务正在运行，请先停止服务再重命名服务！");
            }
            File newDir = FileUtils.getFile(SettingUtils.getWorkspace(), setting.getName());
            if (newDir.exists()) {
                throw new JarbootRunException(setting.getName() + "已经存在，重命名失败！");
            }
            String prePath = pre.getPath();
            if (!FileUtils.getFile(prePath).renameTo(FileUtils.getFile(path))) {
                throw new JarbootRunException("重命名服务失败！");
            }
        }
    }

    private void saveSettingProperties(File file, Properties properties) {
        boolean isNew = false;
        if (Files.notExists(file.getParentFile().toPath())) {
            try {
                FileUtils.forceMkdir(file.getParentFile());
                isNew = true;
            } catch (IOException e) {
                throw new JarbootRunException("创建服务目录失败！" + e.getMessage(), e);
            }
        }
        PropertyFileUtils.storeProperties(file, properties);
        if (isNew) {
            MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
        }
    }

    private void checkAndSet(String path, ServiceSetting setting, Properties prop) {
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
            path = SettingUtils.getPath(SettingUtils.getServicePath(serviceName), file);
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
            path = SettingUtils.getPath(SettingUtils.getServicePath(serviceName), file);
        }
        File f = path.toFile();
        if (!f.exists()) {
            if (StringUtils.isEmpty(content)) {
                return;
            }
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

    @Override
    public void registerSubscriber(Subscriber<WorkspaceChangeEvent> subscriber) {
        NotifyReactor.getInstance().registerSubscriber(subscriber);
    }

    @Override
    public void deregisterSubscriber(Subscriber<WorkspaceChangeEvent> subscriber) {
        NotifyReactor.getInstance().deregisterSubscriber(subscriber);
    }

    private File getConfAndCheck(String p) {
        File file = SettingUtils.getServiceSettingFile(p);
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
