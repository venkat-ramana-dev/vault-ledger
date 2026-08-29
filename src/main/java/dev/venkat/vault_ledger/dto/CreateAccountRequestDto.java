package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateAccountRequestDto(

        @NotBlank(message = "Account name cannot be Empty.")
        @Size(min = 2, max = 120, message = "Account name must be between 2 and 120 characters.")
        String accountHolderName,


        @PositiveOrZero(message = "Initial deposit cannot be negative.")
        BigDecimal initialDeposit) { }