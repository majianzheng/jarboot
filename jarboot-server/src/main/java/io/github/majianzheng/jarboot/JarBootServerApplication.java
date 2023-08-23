package io.github.majianzheng.jarboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring boot main function.
 * @author majianzheng
 */
@SpringBootApplication(scanBasePackages = "io.github.majianzheng.jarboot")
@PropertySource(value={"file:${JARBOOT_HOME}/conf/jarboot.properties"}, ignoreResourceNotFound=true)
public class JarBootServerApplication {

	public static void main(String[] args) {
		//启动环境检查
		SpringApplication.run(JarBootServerApplication.class, args);
	}
}
