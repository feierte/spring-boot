/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.demo.orm.mybatis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
/**
 *
 * @author Jie Zhao
 * @date 2024/11/3 13:23
 */
@SpringBootApplication
public class MybatisSpringApplication {

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		// todo: builder 中的 main 方法和 sources 方法有什么区别？
		SpringApplication application = builder
				.main(MybatisSpringApplication.class)
				.sources(MybatisSpringApplication.class)
				.build(args);
		application.setAdditionalProfiles("mybatis");
		ConfigurableApplicationContext context = application.run(args);

		String name = context.getEnvironment().getProperty("spring.application.name");
		System.out.println(name);
	}
}
