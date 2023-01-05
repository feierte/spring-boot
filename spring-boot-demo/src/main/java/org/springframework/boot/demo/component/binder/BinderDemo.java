package org.springframework.boot.demo.component.binder;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Jie Zhao
 * @date 2023/1/5 21:43
 */
public class BinderDemo {

	public static void main(String[] args) {
		Environment environment = new StandardEnvironment();

		Binder binder = Binder.get(environment);

	}
}
