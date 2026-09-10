package com.fedstack.spending.web;

import com.fedstack.spending.application.CreateTransactionCommand;
import com.fedstack.spending.application.TransactionCreationService;
import com.fedstack.spending.persistence.TransactionEntity;
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

		TransactionEntity created = transactionCreationService.createTransaction(userId, command);

		URI location = uriBuilder.replacePath("/api/v1/transactions/{id}")
				.buildAndExpand(created.getId())
				.toUri();

		CreateTransactionResponse body = new CreateTransactionResponse(
				created.getId(),
				created.getAccount().getId(),
				created.getMerchant().getId(),
				created.getCategory().getId(),
				created.getAmount().toPlainString(),
				created.getDirection(),
				created.getOccurredOn(),
				created.getDescription(),
				created.getCreatedAt()
		);

		return ResponseEntity.created(location).body(body);
	}
}
