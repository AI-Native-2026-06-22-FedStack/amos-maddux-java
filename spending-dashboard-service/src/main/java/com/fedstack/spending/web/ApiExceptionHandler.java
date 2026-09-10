package com.fedstack.spending.web;

import com.fedstack.spending.application.AccountNotOwnedException;
import com.fedstack.spending.application.DuplicateTransactionException;
import com.fedstack.spending.application.TransactionReferenceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps failures from the {@code /api/v1/dashboard}, {@code /api/v1/transactions},
 * and {@code /api/v1/insights/stream} operations to RFC 9457 Problem Details.
 * Scoped to this package's exception types only; the Lesson 6 auth boundary
 * keeps its own error shape via {@code AuthExceptionHandler}.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {
	@ExceptionHandler(MalformedRequestException.class)
	ProblemDetail malformedRequest(MalformedRequestException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Malformed Request", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail malformedParameter() {
		return problem(HttpStatus.BAD_REQUEST, "Malformed Request", "request parameter is malformed");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.orElse("request body failed validation");
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", detail);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail invalidArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", exception.getMessage());
	}

	@ExceptionHandler(TransactionReferenceNotFoundException.class)
	ProblemDetail referenceNotFound(TransactionReferenceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage());
	}

	@ExceptionHandler(AccountNotOwnedException.class)
	ProblemDetail accountNotOwned(AccountNotOwnedException exception) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage());
	}

	@ExceptionHandler(DuplicateTransactionException.class)
	ProblemDetail duplicateTransaction(DuplicateTransactionException exception) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		return problemDetail;
	}
}
