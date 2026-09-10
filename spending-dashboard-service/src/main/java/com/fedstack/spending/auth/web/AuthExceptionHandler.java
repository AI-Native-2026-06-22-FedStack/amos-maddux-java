package com.fedstack.spending.auth.web;

import com.fedstack.spending.auth.application.AuthFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
	@ExceptionHandler(AuthFailureException.class)
	ResponseEntity<AuthErrorResponse> authenticationFailed() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new AuthErrorResponse("invalid_credentials"));
	}
}
