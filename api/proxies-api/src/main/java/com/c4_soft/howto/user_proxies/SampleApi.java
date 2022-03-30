package com.c4_soft.howto.user_proxies;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.c4_soft.howto.user_proxies.domain.Proxy;
import com.c4_soft.howto.user_proxies.exceptions.CustomExceptionHandler;
import com.c4_soft.howto.user_proxies.jpa.ProxyRepository;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.OidcServletApiSecurityConfig;

@SpringBootApplication(scanBasePackageClasses = { SampleApi.class, CustomExceptionHandler.class, OidcServletApiSecurityConfig.class })
@EnableJpaRepositories(basePackageClasses = { ProxyRepository.class })
@EntityScan(basePackageClasses = { Proxy.class })
@EnableTransactionManagement
public class SampleApi {
	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleApi.class).web(WebApplicationType.SERVLET).run(args);
	}
}
