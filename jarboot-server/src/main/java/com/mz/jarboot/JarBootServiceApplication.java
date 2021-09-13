package com.mz.jarboot;

import com.mz.jarboot.common.VersionUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.service.TaskWatchService;
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
@PropertySource(value={"classpath:jarboot.properties", "file:${jarboot.home}/conf/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作空间路径
		String userHome = System.getProperty("user.home");
		String wsHome = userHome + File.separator + CommonConst.JARBOOT_NAME;
		//初始化工作目录
		System.setProperty(CommonConst.WORKSPACE_HOME, wsHome);
		String homePath = System.getenv("JARBOOT_HOME");
		if (null == homePath || homePath.isEmpty()) {
			homePath = wsHome;
		}
		//初始化当前目录
		System.setProperty(CommonConst.JARBOOT_HOME, homePath);
		System.setProperty("application.version", "v" + VersionUtils.version);
		//启动环境检查，若不符合环境要求则弹出swing提示框提醒问题
		CheckBeforeStart.check();

		ApplicationContext context = SpringApplication.run(JarBootServiceApplication.class, args);
		ApplicationContextUtils.init(context);
		TaskWatchService taskWatchService = context.getBean(TaskWatchService.class);
		taskWatchService.init();
	}
}
