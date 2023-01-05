package org.springframework.boot.demo.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jie Zhao
 * @date 2021/11/2 18:22
 */
@RestController
public class HelloController {

	@RequestMapping("/hello")
	public String hello() {
		return "hello world!!!";
	}
}
