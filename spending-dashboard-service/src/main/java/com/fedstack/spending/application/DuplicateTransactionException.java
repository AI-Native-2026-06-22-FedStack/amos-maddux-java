package com.fedstack.spending.application;

public class DuplicateTransactionException extends RuntimeException {
	public DuplicateTransactionException(String message) {
		super(message);
	}
}
