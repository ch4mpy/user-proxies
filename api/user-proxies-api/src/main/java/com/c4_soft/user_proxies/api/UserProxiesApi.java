package com.c4_soft.user_proxies.api;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.c4_soft.user_proxies.api.exceptions.CustomExceptionHandler;

@SpringBootApplication(scanBasePackageClasses = { UserProxiesApi.class, CustomExceptionHandler.class })
public class UserProxiesApi {
	public static void main(String[] args) {
		new SpringApplicationBuilder(UserProxiesApi.class).web(WebApplicationType.SERVLET).run(args);
	}
}
