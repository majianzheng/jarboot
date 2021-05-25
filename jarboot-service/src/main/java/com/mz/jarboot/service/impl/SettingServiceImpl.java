package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.dto.GlobalSettingDTO;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.service.SettingService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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
        String path = SettingUtils.getServerSettingFilePath(server);
        File file = new File(path);
        if (!file.exists()) {
            try {
                boolean rlt = file.createNewFile();
                if (!rlt) {
                    logger.debug("Config file({}) is already exist.", path);
                }
            } catch (IOException e) {
                throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
            }
        }
        Map<String, String> prop = new HashMap<>();
        String jar = setting.getJar();
        if (StringUtils.isNotEmpty(jar)) {
            String jarPath = SettingUtils.getServerPath(server) + File.separator + jar;
            File jarFile = new File(jarPath);
            if (jarFile.exists()) {
                prop.put("jar", jar);
            } else {
                throw new MzException(ResultCodeConst.NOT_EXIST, String.format("jar文件(%s)不存在！", jar));
            }
        }
        if (null != setting.getJvm()) {
            prop.put("jvm", setting.getJvm());
        }
        if (null != setting.getArgs()) {
            prop.put("args", setting.getArgs());
        }
        if (null != setting.getPriority()) {
            prop.put("priority", setting.getPriority().toString());
        }
        if (null != setting.getDaemon()) {
            prop.put("daemon", setting.getDaemon().toString());
        }
        if (null != setting.getJarUpdateWatch()) {
            prop.put("jarUpdateWatch", setting.getJarUpdateWatch().toString());
        }
        PropertyFileUtils.writeProperty(file, prop);
    }

    @Override
    public GlobalSettingDTO getGlobalSetting() {
        return SettingUtils.getGlobalSetting();
    }

    @Override
    public void submitGlobalSetting(GlobalSettingDTO setting) {
        if (setting.getMaxStartTime() < 3000) {
            throw new MzException(ResultCodeConst.INVALID_PARAM,
                    String.format("最小启动时间(%d)应不小于3000！", setting.getMaxStartTime()));
        }
        SettingUtils.updateGlobalSetting(setting);
    }

}
