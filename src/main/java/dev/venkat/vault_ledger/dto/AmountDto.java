package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AmountDto(

        @NotNull(message = "Amount cannot be null.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount) {
}
