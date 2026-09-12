package com.fedstack.spending.web;

import com.fedstack.spending.application.CreateTransactionCommand;
import com.fedstack.spending.application.CreatedTransaction;
import com.fedstack.spending.application.TransactionCreationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
	private final TransactionCreationService transactionCreationService;

	public TransactionController(TransactionCreationService transactionCreationService) {
		this.transactionCreationService = Objects.requireNonNull(
				transactionCreationService,
				"transactionCreationService must not be null"
		);
	}

	@PostMapping
	ResponseEntity<CreateTransactionResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateTransactionRequest request,
			UriComponentsBuilder uriBuilder
	) {
		long userId = Long.parseLong(jwt.getSubject());
		CreateTransactionCommand command = new CreateTransactionCommand(
				request.accountId(),
				request.merchantId(),
				request.categoryId(),
				request.amount(),
				request.direction(),
				request.occurredOn(),
				request.description()
		);

		CreatedTransaction created = transactionCreationService.createTransaction(userId, command);

		URI location = uriBuilder.replacePath("/api/v1/transactions/{id}")
				.buildAndExpand(created.id())
				.toUri();

		CreateTransactionResponse body = new CreateTransactionResponse(
				created.id(),
				created.accountId(),
				created.merchantId(),
				created.categoryId(),
				created.amount().toPlainString(),
				created.direction(),
				created.occurredOn(),
				created.description(),
				created.createdAt()
		);

		return ResponseEntity.created(location).body(body);
	}
}
