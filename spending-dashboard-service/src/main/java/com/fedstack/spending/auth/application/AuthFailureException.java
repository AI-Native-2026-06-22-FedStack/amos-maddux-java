package com.fedstack.spending.auth.application;

public class AuthFailureException extends RuntimeException {
	public AuthFailureException() {
		super("Authentication failed");
	}
}
