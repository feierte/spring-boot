package org.springframework.boot.demo.component.properties;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * @author Jie Zhao
 * @date 2024/9/16 11:09
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "org.springframework.boot.demo.component.properties")
public class PropertiesBinderDemo {


	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(PropertiesBinderDemo.class, args);
		AppProperties appProperties = applicationContext.getBean(AppProperties.class);
		log.info("app properties: {}", appProperties);
	}

	@Configuration
	@EnableConfigurationProperties
	static class PropertiesConfiguration {
	}

	@Data
	@ToString
	@Component
	@ConfigurationProperties(prefix = "app", ignoreUnknownFields = true)
	// 可以配合 Validation 一起使用，校验属性合法性
	@Validated // 开启校验
	static class AppProperties {
		@NotEmpty
		private String name;
		private int age;
		private String version;
		private boolean enabled;
		private List<String> features;
		private Map<String, String> settings;
	}
}
