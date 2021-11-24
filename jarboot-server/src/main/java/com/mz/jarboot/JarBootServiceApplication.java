package com.mz.jarboot;

import com.mz.jarboot.common.VersionUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.event.ApplicationContextUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import java.io.File;

/**
 * Spring boot main function.
 * @author majianzheng
 */
@SpringBootApplication(scanBasePackages = "com.mz.jarboot")
@PropertySource(value={"classpath:jarboot.properties", "file:${JARBOOT_HOME}/conf/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作目录
		String homePath = System.getenv(CommonConst.JARBOOT_HOME);
		if (null == homePath || homePath.isEmpty()) {
			homePath = System.getProperty(CommonConst.JARBOOT_HOME, null);
			if (null == homePath) {
				homePath = System.getProperty("user.home") + File.separator + CommonConst.JARBOOT_NAME;
			}
		}
		System.setProperty(CommonConst.JARBOOT_HOME, homePath);
		System.setProperty("application.version", "v" + VersionUtils.version);
		//启动环境检查，若不符合环境要求则弹出swing提示框提醒问题
		CheckBeforeStart.check();

		ApplicationContext context = SpringApplication.run(JarBootServiceApplication.class, args);
		ApplicationContextUtils.init(context);
	}
}
