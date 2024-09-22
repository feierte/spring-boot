package org.springframework.boot.demo.web.server.tomcat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Collections;
import java.util.Properties;

/**
 * @author Jie Zhao
 * @date 2024/9/21 18:12
 */
@SpringBootApplication(scanBasePackages = {"org.springframework.boot.demo.web"})
public class TomcatSpringApplication {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication();

		/*
		 * 这种方式可以在 springboot 启动之前设置 active profiles
		 */
//		Properties props = new Properties();
//		props.setProperty("spring.profiles.active", "tomcat");
//		springApplication.setDefaultProperties(props);

		/*
		 * 这种方式也可以在 springboot 启动之前设置 active profiles
		 */
		springApplication.setAdditionalProfiles("tomcat");

		springApplication.addPrimarySources(Collections.singletonList(TomcatSpringApplication.class));
		springApplication.run(args);
	}
}
