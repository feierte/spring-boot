/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.demo.properties.binder;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import lombok.Data;
import lombok.ToString;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@SpringBootApplication
public class BinderSpringApplication {

	@Value("${myapp.username}")
	private String username;

	@Value("${myapp.password}")
	private String password;

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		// todo: builder 中的 main 方法和 sources 方法有什么区别？
		builder.profiles("config")
				.main(BinderSpringApplication.class)
				.sources(BinderSpringApplication.class);
		SpringApplication application = builder.build(args);
		ConfigurableApplicationContext context = application.run();

		BinderSpringApplication bean = context.getBean(BinderSpringApplication.class);
		System.out.println(bean.username + ": " + bean.password);
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
