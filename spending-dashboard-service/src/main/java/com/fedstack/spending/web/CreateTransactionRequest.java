package com.fedstack.spending.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
		@NotNull Long accountId,
		@NotNull Long merchantId,
		@NotNull Long categoryId,
		@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@NotNull @Pattern(regexp = "CREDIT|DEBIT") String direction,
		@NotNull LocalDate occurredOn,
		@NotNull @Size(min = 1, max = 255) String description
) {
}
