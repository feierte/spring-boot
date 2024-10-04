package org.springframework.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Jie Zhao
 * @date 2024/10/3 16:13
 */
@SpringBootApplication
public class Main {
	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		builder.sources(Main.class)
				.main(Main.class)
				.logStartupInfo(true)
				.build(args)
				.run(args);

//		SpringApplication.run(Main.class, args);
		System.out.println("Hello world!");
	}
}