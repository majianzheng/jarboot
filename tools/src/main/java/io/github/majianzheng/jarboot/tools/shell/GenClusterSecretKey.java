package io.github.majianzheng.jarboot.tools.shell;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成默认的集群key
 * @author mazheng
 */
public class GenClusterSecretKey {
    public static void main(String[] args) throws IOException {
        String home = System.getProperty(CommonConst.JARBOOT_HOME);
        if (StringUtils.isEmpty(home)) {
            AnsiLog.error("JARBOOT_HOME is not set!");
            return;
        }
        String key = StringUtils.randomString(64);
        final String str = "cluster-secret-key";
        AnsiLog.info("gen cluster key:\n{}", key);
        final String secretKeyLine = String.format("%s=%s", str, key);
        List<String> content = new ArrayList<>();
        File conf = FileUtils.getFile(home, "conf", "cluster.conf");
        boolean no = true;
        if (conf.exists()) {
            List<String> lines = FileUtils.readLines(conf, StandardCharsets.UTF_8);

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith(str)) {
                    content.add(secretKeyLine);
                    no = false;
                } else {
                    content.add(line);
                }
            }
        }
        if (no) {
            content.add(secretKeyLine);
        }
        FileUtils.writeLines(conf, StandardCharsets.UTF_8.name(), content, false);
    }
}
