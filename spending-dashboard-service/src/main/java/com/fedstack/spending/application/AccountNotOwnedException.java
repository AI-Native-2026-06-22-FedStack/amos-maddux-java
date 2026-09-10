package com.fedstack.spending.application;

public class AccountNotOwnedException extends RuntimeException {
	public AccountNotOwnedException(String message) {
		super(message);
	}
}
