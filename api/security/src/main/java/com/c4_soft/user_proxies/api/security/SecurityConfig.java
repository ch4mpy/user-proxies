package com.c4_soft.user_proxies.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.config.OAuth2AuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.OAuth2AuthenticationFactory;
import com.c4_soft.springaddons.security.oauth2.spring.C4MethodSecurityExpressionHandler;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	OAuth2AuthenticationFactory authenticationBuilder(OAuth2AuthoritiesConverter authoritiesConverter) {
		return (bearerString, claims) -> new ProxiesAuthentication(new ProxiesClaimSet(claims),
				authoritiesConverter.convert(claims), bearerString);
	}

	@Bean
	MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		return new C4MethodSecurityExpressionHandler(ProxiesMethodSecurityExpressionRoot::new);
	}
}