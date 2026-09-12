package com.fedstack.spending.web;

public class MalformedRequestException extends RuntimeException {
	public MalformedRequestException(String message) {
		super(message);
	}
}
