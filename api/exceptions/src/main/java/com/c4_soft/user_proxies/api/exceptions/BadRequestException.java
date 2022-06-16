package com.c4_soft.user_proxies.api.exceptions;

public class BadRequestException extends RuntimeException {
	private static final long serialVersionUID = -5775942383747182419L;

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequestException(String message) {
		super(message);
	}

}
