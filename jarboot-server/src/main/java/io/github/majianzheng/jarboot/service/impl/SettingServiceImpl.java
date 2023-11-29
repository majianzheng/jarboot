package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
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
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
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
    @Autowired
    private TaskRunCache taskRunCache;

    @Override
    public ServiceSetting getServiceSetting(String serviceName) {
        ServiceSetting setting =  PropertyFileUtils.getServiceSetting(SettingUtils.getCurrentUserDir(), serviceName);
        setting.setVmContent(this.getVmOptions(serviceName, setting.getVm()));
        setting.setHost(ClusterClientManager.getInstance().getSelfHost());
        return setting;
    }

    @Override
    public void submitServiceSetting(ServiceSetting setting) {
        checkSetting(setting);
        if (StringUtils.isEmpty(setting.getVm())) {
            setting.setVm(SettingPropConst.DEFAULT_VM_FILE);
        }
        final String path = SettingUtils.getServicePath(setting.getName());
        String sid = SettingUtils.createSid(path);
        if (StringUtils.isNotEmpty(setting.getSid()) && !setting.getSid().equals(sid)) {
            // 发生了重命名，获取工作空间下所有服务
            renameService(setting, path);
            MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
        } else {
            ServiceSetting oldSetting = PropertyFileUtils.getServiceSettingBySid(sid);
            if (null != oldSetting && !Objects.equals(oldSetting.getGroup(), setting.getGroup())) {
                MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
            }
        }
        File settingFile = SettingUtils.getServiceSettingFile(path);
        fillSettingProperties(setting, path);
        boolean isNew = false;
        if (Files.notExists(settingFile.getParentFile().toPath())) {
            try {
                FileUtils.forceMkdir(settingFile.getParentFile());
                isNew = true;
            } catch (IOException e) {
                throw new JarbootRunException("创建服务目录失败！" + e.getMessage(), e);
            }
        }
        // 保存vmContent
        saveVmOptions(setting.getName(), setting.getVm(), setting.getVmContent());
        setting.setVmContent(null);
        setting.setUserDir(null);
        setting.setSid(null);
        setting.setHost(null);
        setting.setLastModified(null);
        saveSettingProperties(settingFile, setting);
        if (isNew) {
            MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
        }
        //更新缓存配置，根据文件时间戳判定是否更新了
        PropertyFileUtils.getServiceSetting(SettingUtils.getCurrentUserDir(), setting.getName());
    }

    private void checkSetting(ServiceSetting setting) {
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
            if (StringUtils.isEmpty(setting.getCron())) {
                throw new JarbootRunException("cron配置为空！");
            }
            if (taskRunCache.isScheduling(setting.getSid())) {
                MessageUtils.warn("服务" + setting.getName() + "正在计划中，重启后生效当前配置！");
            }
        } else {
            if (taskRunCache.isScheduling(setting.getSid())) {
                taskRunCache.removeScheduleTask(setting);
                MessageUtils.info("服务" + setting.getName() + "已移除定时任务计划");
            }
        }
    }

    private void fillSettingProperties(ServiceSetting setting, String path) {
        String command = setting.getCommand();
        if (null == command) {
            command = StringUtils.EMPTY;
        } else {
            command = command.replace('\n', ' ');
            setting.setCommand(command);
        }
        setting.setCommand(command);
        String vm = setting.getVm();
        if (StringUtils.isEmpty(vm)) {
            setting.setVm(SettingPropConst.DEFAULT_VM_FILE);
        } else {
            if (!SettingPropConst.DEFAULT_VM_FILE.equals(vm)) {
                checkFileExist(vm, path);
            }
        }
        checkAndSet(path, setting);
        if (null == setting.getDaemon()) {
            setting.setDaemon(true);
        }
        if (null == setting.getFileUpdateWatch()) {
            setting.setFileUpdateWatch(true);
        }
        if (null == setting.getGroup()) {
            setting.setGroup(StringUtils.EMPTY);
        }
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

    private void saveSettingProperties(File file, ServiceSetting setting) {
        String json = JsonUtils.toPrettyJsonString(setting);
        try {
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootException("写入配置文件失败！" + e.getMessage(), e);
        }
    }

    private void checkAndSet(String path, ServiceSetting setting) {
        String workDirectory = setting.getWorkDirectory();
        if (StringUtils.isEmpty(workDirectory)) {
            setting.setWorkDirectory(StringUtils.EMPTY);
        } else {
            checkDirExist(workDirectory);
        }

        String jdkPath = setting.getJdkPath();
        if (StringUtils.isEmpty(jdkPath)) {
            setting.setJdkPath(StringUtils.EMPTY);
        } else {
            String javaFile = jdkPath + File.separator + CommonConst.BIN_NAME +
                    File.separator + CommonConst.JAVA_CMD;
            if (OSUtils.isWindows()) {
                javaFile += CommonConst.EXE_EXT;
            }

            checkFileExist(javaFile, path);
        }

        String env = setting.getEnv();
        if (PropertyFileUtils.checkEnvironmentVar(env)) {
            if (null == env) {
                env = StringUtils.EMPTY;
            }
            setting.setEnv(env);
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
