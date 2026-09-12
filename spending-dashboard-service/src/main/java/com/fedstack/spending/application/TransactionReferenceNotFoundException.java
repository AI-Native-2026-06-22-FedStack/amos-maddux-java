package com.fedstack.spending.application;

public class TransactionReferenceNotFoundException extends RuntimeException {
	public TransactionReferenceNotFoundException(String message) {
		super(message);
	}
}
