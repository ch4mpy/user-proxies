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
@WithSecurityContext(factory = WithProxiesAuth.AuthenticationFactory.class)
public @interface WithProxiesAuth {

	@AliasFor("authorities")
	String[] value() default { "ROLE_USER" };

	@AliasFor("value")
	String[] authorities() default { "ROLE_USER" };

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Grant[] grants() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	public static final class AuthenticationFactory
			extends AbstractAnnotatedAuthenticationBuilder<WithProxiesAuth, ProxiesAuthentication> {
		@Override
		public ProxiesAuthentication authentication(WithProxiesAuth annotation) {
			final var claims = super.claims(annotation.claims());
			@SuppressWarnings("unchecked")
			final var proxiesclaim = Optional.ofNullable((Map<String, List<String>>) claims.get("proxies"))
					.orElse(new HashMap<>());
			for (final var p : annotation.grants()) {
				proxiesclaim.put(p.onBehalfOf(), List.of(p.can()));
			}
			claims.put("proxies", proxiesclaim);
			final var token = new OidcToken(claims);
			return new ProxiesAuthentication(token, super.authorities(annotation.authorities()),
					proxiesclaim.entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey,
									e -> new Proxy(e.getKey(), token.getSubject(), e.getValue()))),
					annotation.bearerString());
		}
	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Grant {

		String onBehalfOf();

		String[] can() default {};
	}
}