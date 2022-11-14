package com.c4_soft.user_proxies.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.c4_soft.user_proxies.api.exceptions.CustomExceptionHandler;
import com.c4_soft.user_proxies.api.security.SecurityConfig;

@SpringBootApplication(scanBasePackageClasses = { GreetApi.class, CustomExceptionHandler.class, SecurityConfig.class })
public class GreetApi {

	public static void main(String[] args) {
		SpringApplication.run(GreetApi.class, args);
	}

}