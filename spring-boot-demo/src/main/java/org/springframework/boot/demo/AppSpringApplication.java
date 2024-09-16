package org.springframework.boot.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class AppSpringApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(AppSpringApplication.class, args);
		System.out.println("Hello Spring Boot");
	}

	@EventListener(WebServerInitializedEvent.class)
	public void onWebServerReady(WebServerInitializedEvent event) {
		log.info("当前 WebServer 实现类为：{}", event.getWebServer().getClass().getName());
	}
}
