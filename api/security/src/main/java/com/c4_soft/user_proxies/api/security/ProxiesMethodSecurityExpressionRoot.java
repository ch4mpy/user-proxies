package com.c4_soft.user_proxies.api.security;

import java.util.Objects;
import java.util.Set;

import com.c4_soft.springaddons.security.oauth2.spring.C4MethodSecurityExpressionRoot;

final class ProxiesMethodSecurityExpressionRoot extends C4MethodSecurityExpressionRoot {

	public boolean is(String preferredUsername) {
		return Objects.equals(getAuthentication().getName(), preferredUsername);
	}

	public Proxy onBehalfOf(String proxiedUsername) {
		return get(ProxiesAuthentication.class).map(a -> a.getProxyFor(proxiedUsername))
				.orElse(new Proxy(proxiedUsername, getAuthentication().getName(), Set.of()));
	}

	public boolean isNice() {
		return hasAnyAuthority("ROLE_NICE_GUY", "SUPER_COOL");
	}
}