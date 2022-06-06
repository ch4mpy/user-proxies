package com.c4_soft.user_proxies.api;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.c4_soft.user_proxies.api.domain.User;
import com.c4_soft.user_proxies.api.exceptions.CustomExceptionHandler;
import com.c4_soft.user_proxies.api.jpa.UserRepository;

@SpringBootApplication(scanBasePackageClasses = { UserProxiesApi.class, CustomExceptionHandler.class })
public class UserProxiesApi {
	public static void main(String[] args) {
		new SpringApplicationBuilder(UserProxiesApi.class).web(WebApplicationType.SERVLET).run(args);
	}

	@EnableJpaRepositories(basePackageClasses = { UserRepository.class })
	@EntityScan(basePackageClasses = { User.class })
	@EnableTransactionManagement
	public static class PersistenceConfig {
	}
}
