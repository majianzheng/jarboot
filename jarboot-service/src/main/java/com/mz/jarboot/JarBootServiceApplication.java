package com.mz.jarboot;

import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.utils.SettingUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import java.io.File;
import java.security.CodeSource;

/**
 * Spring boot main function.
 * @author jianzhengma
 */
@SpringBootApplication
@PropertySource(value={"classpath:jarboot.properties", "file:${workspace.home}/jarboot.properties"}, ignoreResourceNotFound=true)
@ComponentScan(basePackages= {"com.mz.jarboot.service", "com.mz.jarboot.config", "com.mz.jarboot.task","com.mz.jarboot.controller", "com.mz.jarboot.ws"})
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作空间路径
		String userHome = System.getProperty("user.home");
		String wsHome = userHome + File.separator + "jarboot";
		System.setProperty(CommonConst.WORKSPACE_HOME, wsHome); //初始化工作目录
		System.setProperty(CommonConst.JARBOOT_HOME, getCurrentPath()); //初始化当前目录

		//启动环境检查，若不符合环境要求则坦诚swing提示框提醒问题
		CheckBeforeStart.check();

		ApplicationContext context = SpringApplication.run(JarBootServiceApplication.class, args);
		ApplicationContextUtils.setContext(context);
		TaskWatchService taskWatchService = context.getBean(TaskWatchService.class);
		taskWatchService.init();
	}

	private static String getCurrentPath() {
		CodeSource codeSource = SettingUtils.class.getProtectionDomain().getCodeSource();
		try {
			File curJar = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
			return curJar.getParent();
		} catch (Exception e) {
			//ignore
		}
		return "./";
	}
}
