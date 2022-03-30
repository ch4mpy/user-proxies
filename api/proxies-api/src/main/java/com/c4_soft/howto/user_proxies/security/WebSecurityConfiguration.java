package com.c4_soft.howto.user_proxies.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2OidcTokenConverter;

@Configuration
public class WebSecurityConfiguration {

	@Bean
	public SynchronizedJwt2OidcTokenConverter<ProxiesOidcToken> tokenConverter() {
		return (var jwt) -> new ProxiesOidcToken(jwt.getClaims());
	}

}
