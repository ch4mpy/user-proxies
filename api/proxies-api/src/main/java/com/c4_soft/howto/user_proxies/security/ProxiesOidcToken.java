package com.c4_soft.howto.user_proxies.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;

public class ProxiesOidcToken extends OidcToken {
	private static final long serialVersionUID = 4108750848561919233L;

	public ProxiesOidcToken(Map<String, Object> claims) {
		super(claims);
	}

	@SuppressWarnings("unchecked")
	public Map<String, List<String>> getProxies() {
		return Optional.ofNullable((Map<String, List<String>>) get("proxies")).orElse(Map.of());
	}

	public boolean allows(String grantedUserProxy, String grant) {
		final var userGrants = Optional.ofNullable(getProxies().get(grantedUserProxy)).orElse(List.of());
		return userGrants.contains(grant);
	}
}
