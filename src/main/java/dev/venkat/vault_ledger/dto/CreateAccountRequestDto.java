package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateAccountRequestDto(

        @NotBlank(message = "Account name cannot be Empty.")
        @Size(min = 2, max = 120, message = "Account name must be between 2 and 120 characters.")
        String accountHolderName,


        @NotNull(message = "Initial deposit is required")
        @PositiveOrZero(message = "Initial deposit cannot be negative.")
        @Digits(integer = 15, fraction = 4, message = "Initial deposit must have at most 15 integer digits and 4 decimal places")
        BigDecimal initialDeposit) { }