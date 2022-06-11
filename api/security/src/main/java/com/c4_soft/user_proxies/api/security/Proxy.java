package com.c4_soft.user_proxies.api.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class Proxy implements Serializable {
	private static final long serialVersionUID = 6372863392726898339L;
	
	private final String proxiedUsername;
	private final String tenantUsername;
	private final Set<Permission> permissions;

	public Proxy(String proxiedUsername, String tenantUsername, Set<Permission> permissions) {
		this.proxiedUsername = proxiedUsername;
		this.tenantUsername = tenantUsername;
		this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
	}

	public boolean can(Permission permission) {
		return permissions.contains(permission);
	}

	public boolean can(String permission) {
		return this.can(Permission.valueOf(permission));
	}
}