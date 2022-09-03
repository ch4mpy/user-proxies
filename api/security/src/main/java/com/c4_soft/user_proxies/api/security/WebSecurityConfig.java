package com.c4_soft.user_proxies.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.config.OAuth2AuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.config.OAuth2ClaimsConverter;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.OAuth2AuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.spring.C4MethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
	
	@Bean
	OAuth2ClaimsConverter<ProxiesClaimSet> claimsConverter() {
		return ProxiesClaimSet::new;
	}

    @Bean
    OAuth2AuthenticationBuilder<ProxiesAuthentication> authenticationBuilder(
    		OAuth2ClaimsConverter<ProxiesClaimSet> claimsConverter,
    		OAuth2AuthoritiesConverter authoritiesConverter) {
        return (bearerString, claims) -> {
        	final var claimSet = claimsConverter.convert(claims);
        	return new ProxiesAuthentication(claimSet, authoritiesConverter.convert(claims), bearerString);
    	};
    }

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new C4MethodSecurityExpressionHandler(ProxiesMethodSecurityExpressionRoot::new);
    }
}