package com.c4_soft.user_proxies.api.security;

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;

import com.c4_soft.springaddons.security.oauth2.OAuthentication;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProxiesAuthentication extends OAuthentication<ProxiesClaimSet> {
	private static final long serialVersionUID = 6856299734098317908L;

	public ProxiesAuthentication(ProxiesClaimSet claims, Collection<? extends GrantedAuthority> authorities, String bearerString) {
		super(claims, authorities, bearerString);
	}
	
	@Override
	public String getName() {
		return getClaims().getPreferredUsername();
	}
	
	public boolean hasName(String preferredUsername) {
		return Objects.equals(getName(), preferredUsername);
	}

	public Proxy getProxyFor(String proxiedUsername) {
		return this.getClaims().getProxyFor(proxiedUsername);
	}
}