package com.c4_soft.user_proxies.api;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.c4_soft.user_proxies.api.exceptions.CustomExceptionHandler;
import com.c4_soft.user_proxies.api.security.SecurityConfig;

@SpringBootApplication(scanBasePackageClasses = { UserProxiesApi.class, CustomExceptionHandler.class, SecurityConfig.class })
public class UserProxiesApi {
	public static void main(String[] args) {
		new SpringApplicationBuilder(UserProxiesApi.class).web(WebApplicationType.SERVLET).run(args);
	}
}
