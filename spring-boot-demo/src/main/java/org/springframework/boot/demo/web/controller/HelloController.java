package org.springframework.boot.demo.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jie Zhao
 * @date 2021/11/2 18:22
 */
@Slf4j
@RestController
public class HelloController {

	@RequestMapping("/hello")
	public String hello() {
		log.info("HelloController invoked...");
		return "hello world!!!";
	}
}
