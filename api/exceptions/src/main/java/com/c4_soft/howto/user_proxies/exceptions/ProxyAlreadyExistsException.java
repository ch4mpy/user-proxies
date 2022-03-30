package com.c4_soft.howto.user_proxies.exceptions;

public class ProxyAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = -1613853050408592895L;

	public ProxyAlreadyExistsException(String proxiedUserSubject, String grantedUserSubject) {
		super(String.format("A user proxy from %s to %s already exists", proxiedUserSubject, grantedUserSubject));
	}
}
