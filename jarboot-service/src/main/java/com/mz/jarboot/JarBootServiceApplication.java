package com.mz.jarboot;

import com.mz.jarboot.constant.CommonConst;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import java.io.File;

@SpringBootApplication
@PropertySource(value={"classpath:jarboot.properties", "file:${workspace.home}/jarboot.properties"}, ignoreResourceNotFound=true)
@ComponentScan(basePackages= {"com.mz.jarboot.service", "com.mz.jarboot.config","com.mz.jarboot.dao","com.mz.jarboot.controller", "com.mz.jarboot.ws"})
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作空间路径
		String userHome = System.getProperty("user.home");
		String wsHome = userHome + File.separator + "jarboot";
		System.setProperty(CommonConst.WORKSPACE_HOME, wsHome);

		SpringApplication.run(JarBootServiceApplication.class, args);
	}

}
