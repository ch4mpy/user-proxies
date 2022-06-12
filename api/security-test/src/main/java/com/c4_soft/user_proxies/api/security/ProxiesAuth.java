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
import java.util.stream.Stream;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import com.c4_soft.springaddons.security.oauth2.test.annotations.AbstractAnnotatedAuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = ProxiesAuth.AuthenticationFactory.class)
public @interface ProxiesAuth {

	@AliasFor("authorities")
	String[] value() default {};

	@AliasFor("value")
	String[] authorities() default {};

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Grant[] grants() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Grant {

		String onBehalfOf();

		Permission[] can() default { Permission.PROFILE_READ };
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

			return new ProxiesAuthentication(new ProxiesClaimSet(claims), super.authorities(annotation.authorities()), annotation.bearerString());
		}
	}
}