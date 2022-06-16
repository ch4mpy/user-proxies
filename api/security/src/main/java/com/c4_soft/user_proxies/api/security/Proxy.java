package com.c4_soft.user_proxies.api.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.c4_soft.user_proxies.api.web.dto.Grant;

import lombok.Data;

@Data
public class Proxy implements Serializable {
	private static final long serialVersionUID = 6372863392726898339L;
	
	private final String proxiedUsername;
	private final String tenantUsername;
	private final Set<Grant> permissions;

	public Proxy(String proxiedUsername, String tenantUsername, Set<Grant> permissions) {
		this.proxiedUsername = proxiedUsername;
		this.tenantUsername = tenantUsername;
		this.permissions = Collections.unmodifiableSet(permissions);
	}

	public boolean can(Grant permission) {
		return permissions.contains(permission);
	}

	public boolean can(String permission) {
		return this.can(Grant.valueOf(permission));
	}
}