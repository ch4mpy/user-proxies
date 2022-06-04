package com.c4_soft.howto.user_proxies;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.c4_soft.howto.user_proxies.domain.Proxy;
import com.c4_soft.howto.user_proxies.jpa.ProxyRepository;

@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = { ProxyRepository.class })
@EntityScan(basePackageClasses = { Proxy.class })
@EnableTransactionManagement
public class SampleApi {
	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleApi.class).web(WebApplicationType.SERVLET).run(args);
	}
}
