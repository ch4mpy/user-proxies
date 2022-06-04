package com.c4_soft.howto.user_proxies.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;

import com.c4_soft.springaddons.security.oauth2.oidc.OidcAuthentication;
import com.c4_soft.springaddons.security.oauth2.oidc.OidcToken;

public class ProxiesAuthentication extends OidcAuthentication<OidcToken> {
	public ProxiesAuthentication(OidcToken token, Collection<? extends GrantedAuthority> authorities,
			String bearerString) {
		super(token, authorities, bearerString);
	}

	private static final long serialVersionUID = 4108750848561919233L;

	@SuppressWarnings("unchecked")
	public Map<String, List<String>> getProxies() {
		return Optional.ofNullable((Map<String, List<String>>) getToken().get("proxies")).orElse(Map.of());
	}

	public boolean allows(String grantedUserProxy, String grant) {
		final var userGrants = Optional.ofNullable(getProxies().get(grantedUserProxy)).orElse(List.of());
		return userGrants.contains(grant);
	}
}
