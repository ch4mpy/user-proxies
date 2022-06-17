package com.c4_soft.user_proxies.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.c4_soft.springaddons.security.oauth2.OpenidClaimSet;
import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProxiesClaimSet extends OpenidClaimSet {
	private static final long serialVersionUID = 38784488788537111L;

	private final Map<String, Proxy> proxies;

	public ProxiesClaimSet(Map<String, Object> claims) {
		super(claims);
		this.proxies = getProxies(this).stream().collect(Collectors.toMap(Proxy::getProxiedUsername, p -> p));
	}

	private static List<Proxy> getProxies(OpenidClaimSet claims) {
		@SuppressWarnings("unchecked")
		final var proxiesClaim = (Map<String, List<String>>) claims.get("proxies");
		if (proxiesClaim == null) {
			return List.of();
		}
		return proxiesClaim.entrySet().stream().map(e -> new Proxy(e.getKey(), claims.getPreferredUsername(), e.getValue().stream().map(Grant::valueOf).collect(Collectors.toSet()))).toList();
	}

	public Proxy getProxyFor(String username) {
		return proxies.getOrDefault(username, new Proxy(username, getName(), Set.of()));
	}
}
