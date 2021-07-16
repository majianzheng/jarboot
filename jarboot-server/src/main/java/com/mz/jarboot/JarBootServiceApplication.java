package com.mz.jarboot;

import com.mz.jarboot.common.VersionUtils;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.service.TaskWatchService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * Spring boot main function.
 * @author jianzhengma
 */
@SpringBootApplication(scanBasePackages = "com.mz.jarboot")
@PropertySource(value={"classpath:jarboot.properties", "file:${jarboot.home}/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作空间路径
		String userHome = System.getProperty("user.home");
		String wsHome = userHome + File.separator + "jarboot";
		System.setProperty(CommonConst.WORKSPACE_HOME, wsHome); //初始化工作目录
		String homePath = getCurrentPath();
		if (null == homePath) {
			homePath = wsHome;
		}
		System.setProperty(CommonConst.JARBOOT_HOME, homePath); //初始化当前目录
		System.setProperty("application.version", "v" + VersionUtils.version);
		//启动环境检查，若不符合环境要求则弹出swing提示框提醒问题
		CheckBeforeStart.check();

		ApplicationContext context = SpringApplication.run(JarBootServiceApplication.class, args);
		ApplicationContextUtils.setContext(context);
		TaskWatchService taskWatchService = context.getBean(TaskWatchService.class);
		taskWatchService.init();
	}

	private static String getCurrentPath() {
		CodeSource codeSource = JarBootServiceApplication.class.getProtectionDomain().getCodeSource();
		File curJar;
		try {
			curJar = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
			String path = curJar.getPath();
			int p = path.lastIndexOf(".jar");
			if (-1 == p) {
				return null;
			}
			//取上级目录
			p = path.lastIndexOf(File.separatorChar, p);
			if (0 == path.indexOf("file:")) {
				if ('\\' == path.charAt(5)) {
					return path.substring(6, p);
				}
				return path.substring(5, p);
			} else {
				return path.substring(0, p);
			}
		} catch (URISyntaxException e) {
			//ignore
		}
		//调试环境，使用用户目录下的 WORKSPACE_HOME
		return null;
	}
}
