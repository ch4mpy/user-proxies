package com.c4_soft.user_proxies.api.exceptions;

public class ProxyUsersUnmodifiableException extends RuntimeException {
	private static final long serialVersionUID = 4419101891617503927L;

	public ProxyUsersUnmodifiableException() {
		super("Proxy users cannot be modified");
	}
}