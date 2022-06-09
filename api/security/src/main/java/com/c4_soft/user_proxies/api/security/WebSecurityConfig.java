package com.c4_soft.user_proxies.api.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2AuthenticationConverter;
import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2OidcTokenConverter;
import com.c4_soft.springaddons.security.oauth2.config.JwtGrantedAuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;
import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	public interface ProxiesConverter extends Converter<Jwt, Map<String, Proxy>> {
	}

	@Bean
	public ProxiesConverter proxiesConverter() {
		return jwt -> {
			@SuppressWarnings("unchecked")
			final var proxiesClaim = (Map<String, List<String>>) jwt.getClaims().get("proxies");
			if (proxiesClaim == null) {
				return Map.of();
			}
			return proxiesClaim.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new Proxy(e.getKey(), jwt.getSubject(), e.getValue())));
		};
	}
	
	@Bean
	public SynchronizedJwt2AuthenticationConverter<ProxiesAuthentication> authenticationConverter(
			JwtGrantedAuthoritiesConverter authoritiesConverter,
			SynchronizedJwt2OidcTokenConverter<OidcToken> tokenConverter,
			ProxiesConverter proxiesConverter) {
		return jwt -> new ProxiesAuthentication(tokenConverter.convert(jwt), authoritiesConverter.convert(jwt), proxiesConverter.convert(jwt), jwt.getTokenValue());
	}
	
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		return new GenericMethodSecurityExpressionHandler<>(ProxiesMethodSecurityExpressionRoot::new);
	}
}