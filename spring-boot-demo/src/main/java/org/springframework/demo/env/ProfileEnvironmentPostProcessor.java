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

package org.springframework.demo.env;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Jie Zhao
 * @date 2024/10/6 9:43
 */
public class ProfileEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private final String additionalLocation;

	public ProfileEnvironmentPostProcessor(String additionalLocation) {
		this.additionalLocation = additionalLocation;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> map = new HashMap<>();
		map.put("spring.config.additional-location", this.additionalLocation);
		propertySources.addLast(new MapPropertySource("config", map));

		System.out.println(">>>>>>>>>" + Arrays.toString(environment.getActiveProfiles()));
	}
}
