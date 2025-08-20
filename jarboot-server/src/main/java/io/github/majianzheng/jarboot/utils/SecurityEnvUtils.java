package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.AesUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 隐私安全的环境变量配置工具类
 * @author majianzheng
 */
@Slf4j
public class SecurityEnvUtils {
    private static final String ENV_FILE = ".boot.env";

    public static List<String> getEnv(String serviceDir) {
        List<String> envs = new ArrayList<>();
        if (StringUtils.isEmpty(serviceDir)) {
            return envs;
        }
        File envFile = FileUtils.getFile(serviceDir, ENV_FILE);
        if (!envFile.exists()) {
            return envs;
        }
        try {
            List<String> lines = FileUtils.readLines(envFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String env = AesUtils.decrypt(line);
                if (env.contains("=")) {
                    envs.add(line);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return envs;
    }

    public void saveEnv(String serviceDir, List<String> envs) {
        if (StringUtils.isEmpty(serviceDir)) {
            return;
        }
        File envFile = FileUtils.getFile(serviceDir, ENV_FILE);
        List<String> encEnvs = envs.stream().filter(StringUtils::isNotEmpty).map(AesUtils::encrypt).toList();
        try {
            FileUtils.writeLines(envFile, encEnvs);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }


    private SecurityEnvUtils() {}
}
