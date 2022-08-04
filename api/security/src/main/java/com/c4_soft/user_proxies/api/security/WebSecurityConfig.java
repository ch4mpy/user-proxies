package com.c4_soft.user_proxies.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.config.ClaimSet2AuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.config.Jwt2ClaimSetConverter;
import com.c4_soft.springaddons.security.oauth2.config.synchronised.SynchronizedJwt2AuthenticationConverter;
import com.c4_soft.springaddons.security.oauth2.spring.C4MethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
	
	@Bean
	Jwt2ClaimSetConverter<ProxiesClaimSet> claimsConverter() {
		return jwt -> new ProxiesClaimSet(jwt.getClaims());
	}

    @Bean
    SynchronizedJwt2AuthenticationConverter<ProxiesAuthentication> authenticationConverter(
    		Jwt2ClaimSetConverter<ProxiesClaimSet> claimsConverter,
    		ClaimSet2AuthoritiesConverter<ProxiesClaimSet> authoritiesConverter) {
        return jwt -> {
        	final var claims = claimsConverter.convert(jwt);
        	return new ProxiesAuthentication(claims, authoritiesConverter.convert(claims), jwt.getTokenValue());
    	};
    }

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new C4MethodSecurityExpressionHandler(ProxiesMethodSecurityExpressionRoot::new);
    }
}