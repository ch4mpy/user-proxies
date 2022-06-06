package com.c4_soft.user_proxies.api.security;

import com.c4_soft.springaddons.security.oauth2.spring.GenericMethodSecurityExpressionRoot;

final class ProxiesMethodSecurityExpressionRoot extends GenericMethodSecurityExpressionRoot<ProxiesAuthentication> {
	public ProxiesMethodSecurityExpressionRoot() {
		super(ProxiesAuthentication.class);
	}

	public Proxy onBehalfOf(String proxiedUserSubject) {
		return getAuth().getProxyFor(proxiedUserSubject);
	}

	public boolean isNice() {
		return hasAnyAuthority("ROLE_NICE_GUY", "SUPER_COOL");
	}
}