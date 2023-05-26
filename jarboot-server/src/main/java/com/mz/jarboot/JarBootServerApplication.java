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
@PropertySource(value={"file:${JARBOOT_HOME}/conf/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServerApplication {

	public static void main(String[] args) {
		//启动环境检查
		AppEnvironment.initAndCheck();
		ApplicationContext context = SpringApplication.run(JarBootServerApplication.class, args);
		ApplicationContextUtils.init(context);
	}
}
