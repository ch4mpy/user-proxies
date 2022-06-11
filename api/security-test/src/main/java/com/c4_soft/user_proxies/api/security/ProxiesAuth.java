package com.c4_soft.user_proxies.api.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;
import com.c4_soft.springaddons.security.oauth2.test.annotations.AbstractAnnotatedAuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = ProxiesAuth.AuthenticationFactory.class)
public @interface ProxiesAuth {

	@AliasFor("authorities")
	String[] value() default { "ROLE_USER" };

	@AliasFor("value")
	String[] authorities() default { "ROLE_USER" };

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Grant[] grants() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Grant {

		String onBehalfOf();

		Permission[] can() default {};
	}

	public static final class AuthenticationFactory
			extends AbstractAnnotatedAuthenticationBuilder<ProxiesAuth, ProxiesAuthentication> {
		@Override
		public ProxiesAuthentication authentication(ProxiesAuth annotation) {
			final var claims = super.claims(annotation.claims());
			@SuppressWarnings("unchecked")
			final var proxiesclaim = Optional.ofNullable((Map<String, List<String>>) claims.get("proxies"))
					.orElse(new HashMap<>());
			for (final var p : annotation.grants()) {
				proxiesclaim.put(p.onBehalfOf(), Stream.of(p.can()).map(Permission::toString).toList());
			}
			claims.put("proxies", proxiesclaim);

			final var token = new OidcToken(claims);
			final var authorities = super.authorities(annotation.authorities());
			final var proxies = proxiesclaim.entrySet().stream()
					.map(e -> new Proxy(e.getKey(), token.getPreferredUsername(), e.getValue().stream().map(Permission::valueOf).collect(Collectors.toSet()))).toList();
			return new ProxiesAuthentication(token, authorities, proxies, annotation.bearerString());
		}
	}
}