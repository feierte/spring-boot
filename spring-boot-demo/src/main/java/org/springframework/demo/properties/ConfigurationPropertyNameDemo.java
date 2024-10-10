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

package org.springframework.demo.properties;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.Assert;

/**
 * @author Jie Zhao
 * @date 2024/10/8 22:11
 */
public class ConfigurationPropertyNameDemo {

//	@Test
	public void testIsLastElementIndexed() {
		/*
		 * spring.main.banner-mode
		 * server.hosts[0].name
		 * log[org.springboot].level
		 */
		Assert.isTrue(ConfigurationPropertyName.of("server.hosts[0]").isLastElementIndexed());
	}
}
