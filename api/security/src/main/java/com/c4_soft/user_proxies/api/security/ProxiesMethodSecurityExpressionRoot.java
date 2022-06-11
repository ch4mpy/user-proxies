package com.c4_soft.user_proxies.api.security;

import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionRoot;

final class ProxiesMethodSecurityExpressionRoot extends GenericMethodSecurityExpressionRoot<ProxiesAuthentication> {
	public ProxiesMethodSecurityExpressionRoot() {
		super(ProxiesAuthentication.class);
	}

	public boolean is(String preferredUsername) {
		return getAuth().hasName(preferredUsername);
	}

	public Proxy onBehalfOf(String proxiedUsername) {
		return getAuth().getProxyFor(proxiedUsername);
	}

	public boolean isNice() {
		return hasAnyAuthority("ROLE_NICE_GUY", "SUPER_COOL");
	}
}