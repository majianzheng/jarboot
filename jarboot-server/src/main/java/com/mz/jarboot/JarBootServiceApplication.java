package com.mz.jarboot;

import com.mz.jarboot.event.ApplicationContextUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring boot main function.
 * @author majianzheng
 */
@SpringBootApplication(scanBasePackages = "com.mz.jarboot")
@PropertySource(value={"classpath:jarboot.properties", "file:${JARBOOT_HOME}/conf/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//启动环境检查，若不符合环境要求则弹出swing提示框提醒问题
		AppEnvironment.initAndCheck();
		ApplicationContext context = SpringApplication.run(JarBootServiceApplication.class, args);
		ApplicationContextUtils.init(context);
	}
}
