package com.c4_soft.howto.user_proxies;

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

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import com.c4_soft.howto.user_proxies.security.ProxiesOidcToken;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcAuthentication;
import com.c4_soft.springaddons.security.oauth2.test.annotations.AbstractAnnotatedAuthenticationBuilder;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithProxiesOidcToken.AuthenticationFactory.class)
public @interface WithProxiesOidcToken {

	@AliasFor("authorities")
	String[] value() default { "ROLE_USER" };

	@AliasFor("value")
	String[] authorities() default { "ROLE_USER" };

	OpenIdClaims claims() default @OpenIdClaims();

	String bearerString() default "machin.truc.chose";

	Grant[] grants() default {};

	@AliasFor(annotation = WithSecurityContext.class)
	TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

	public static final class AuthenticationFactory extends AbstractAnnotatedAuthenticationBuilder<WithProxiesOidcToken, OidcAuthentication<ProxiesOidcToken>> {
		@Override
		public OidcAuthentication<ProxiesOidcToken> authentication(WithProxiesOidcToken annotation) {
			final var claims = super.claims(annotation.claims());
			@SuppressWarnings("unchecked")
			final var proxiesclaim = Optional.ofNullable((Map<String, List<String>>) claims.get("proxies")).orElse(new HashMap<String, List<String>>());
			for (final var p : annotation.grants()) {
				proxiesclaim.put(p.onBehalfOf(), List.of(p.can()));
			}
			claims.put("proxies", proxiesclaim);
			return new OidcAuthentication<>(new ProxiesOidcToken(claims), super.authorities(annotation.authorities()), annotation.bearerString());
		}
	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Grant {

		String onBehalfOf();

		String[] can() default {};
	}
}