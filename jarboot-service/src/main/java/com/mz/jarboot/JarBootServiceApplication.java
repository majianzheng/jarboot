package com.mz.jarboot;

import com.mz.jarboot.constant.SettingConst;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import java.io.File;

@SpringBootApplication
@PropertySource(value={"classpath:jar-boot.properties", "file:${workspace.home}/jar-boot.properties"}, ignoreResourceNotFound=true)
@ComponentScan(basePackages= {"com.mz.jarboot.service", "com.mz.jarboot.config","com.mz.jarboot.dao","com.mz.jarboot.controller"})
public class JarBootServiceApplication {

	public static void main(String[] args) {
		//初始化工作空间路径
		String userHome = System.getProperty("user.home");
		String wsHome = userHome + File.separator + "jar-boot";
		System.setProperty(SettingConst.WORKSPACE_HOME, wsHome);

		SpringApplication.run(JarBootServiceApplication.class, args);
	}

}
