package com.c4_soft.user_proxies.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.c4_soft.user_proxies.api.domain.Proxy;
import com.c4_soft.user_proxies.api.jpa.ProxyRepository;

/**
 * Avoid MethodArgumentConversionNotSupportedException with repos MockBean
 *
 * @author Jérôme Wacongne &lt;ch4mp#64;c4-soft.com&gt;
 */
@TestConfiguration
public class EnableSpringDataWebSupportTestConf {
	@Bean
	WebMvcConfigurer configurer(ProxyRepository sampleRepo) {
		return new WebMvcConfigurer() {
			@Override
			public void addFormatters(FormatterRegistry registry) {
				registry.addConverter(String.class, Proxy.class, id -> sampleRepo.findById(Long.valueOf(id)).get());
			}
		};
	}
}