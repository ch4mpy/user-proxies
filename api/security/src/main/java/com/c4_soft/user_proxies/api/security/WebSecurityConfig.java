package com.c4_soft.user_proxies.api.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2AuthenticationConverter;
import com.c4_soft.springaddons.security.oauth2.SynchronizedJwt2OidcTokenConverter;
import com.c4_soft.springaddons.security.oauth2.config.JwtGrantedAuthoritiesConverter;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;
import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionHandler;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	public interface OidcToken2ProxiesConverter extends Converter<OidcToken, Collection<Proxy>> {
	}

	@Bean
	public OidcToken2ProxiesConverter proxiesConverter() {
		return token -> {
			@SuppressWarnings("unchecked")
			final var proxiesClaim = (Map<String, List<String>>) token.getClaims().get("proxies");
			if (proxiesClaim == null) {
				return List.of();
			}
			return proxiesClaim.entrySet().stream().map(e -> new Proxy(e.getKey(), token.getPreferredUsername(), e.getValue().stream().map(Permission::valueOf).collect(Collectors.toSet()))).toList();
		};
	}
	
	@Bean
	public SynchronizedJwt2AuthenticationConverter<ProxiesAuthentication> authenticationConverter(
			JwtGrantedAuthoritiesConverter authoritiesConverter,
			SynchronizedJwt2OidcTokenConverter<OidcToken> tokenConverter,
			OidcToken2ProxiesConverter proxiesConverter) {
		return jwt ->  {
            final var token = tokenConverter.convert(jwt);
            final var authorities = authoritiesConverter.convert(jwt);
            final var proxies = proxiesConverter.convert(token);
            return new ProxiesAuthentication(token, authorities, proxies, jwt.getTokenValue());
        };
	}
	
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		return new GenericMethodSecurityExpressionHandler<>(ProxiesMethodSecurityExpressionRoot::new);
	}
}