package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@Builder
public record TransferRequestDto(

        @PathVariable
        @NotBlank(message = "AccountNumber cannot be blank.")
        String toAccountNumber,

        @PathVariable
        @NotNull(message = "Amount cannot be null.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount) { }
