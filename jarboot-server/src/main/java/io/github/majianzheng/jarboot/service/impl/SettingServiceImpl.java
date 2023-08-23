package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.pojo.SystemSetting;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.service.FileService;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
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
    @Autowired
    private FileService fileService;

    @Override
    public ServiceSetting getServiceSetting(String serviceName) {
        ServiceSetting setting =  PropertyFileUtils.getServiceSetting(SettingUtils.getCurrentUserDir(), serviceName);
        setting.setVmContent(this.getVmOptions(serviceName, setting.getVm()));
        String path = SettingUtils.getWorkspace() + File.separator + serviceName;
        fileService.getFiles(path, true);
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
        if (!SettingPropConst.SCHEDULE_ONCE.equals(setting.getScheduleType()) &&
                !SettingPropConst.SCHEDULE_LONE.equals(setting.getScheduleType()) &&
                !SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
            throw new JarbootRunException("执行计划类型错误！");
        }
        if (SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
            // 周期执行
            throw new JarbootRunException("定时任务功能暂未实现！");
        }
        if (StringUtils.isEmpty(setting.getVm())) {
            setting.setVm(SettingPropConst.DEFAULT_VM_FILE);
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
        saveSettingProperties(settingFile, properties);
        // 保存vmContent
        saveVmOptions(setting.getName(), setting.getVm(), setting.getVmContent());
        //更新缓存配置，根据文件时间戳判定是否更新了
        PropertyFileUtils.getServiceSetting(SettingUtils.getCurrentUserDir(), setting.getName());
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
        if (StringUtils.isEmpty(vm)) {
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
        properties.setProperty(SettingPropConst.SCHEDULE_TYPE, setting.getScheduleType());
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
        List<ServiceInstance> services = taskRunCache.getServiceList(SettingUtils.getCurrentUserDir());
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
            String prePath = SettingUtils.getServicePath(pre.getName());
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
    public SystemSetting getSystemSetting() {
        return SettingUtils.getSystemSetting();
    }

    @Override
    public void saveSetting(SystemSetting setting) {
        SettingUtils.updateSystemSetting(setting);
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
        if (StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(file)) {
            throw new JarbootException("参数为空.");
        }
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
