package org.springframework.boot.demo.component.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Jie Zhao
 * @date 2024/9/20 20:55
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"org.springframework.boot.demo.web", "org.springframework.boot.demo.component.logging"})
public class LoggingSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoggingSpringApplication.class, args);
		log.info("LoggingSpringApplication started successfully!");
	}
}
