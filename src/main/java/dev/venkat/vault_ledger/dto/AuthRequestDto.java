package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.*;


import java.math.BigDecimal;

public record AuthRequestDto(

        @NotBlank
        @Size(min = 8, max = 50)
        String username,

        @NotBlank
        @Size(min = 8, max = 20)
        String password,

        @NotBlank(message = "Account name cannot be Empty.")
        @Size(min = 2, max = 120, message = "Account name must be between 2 and 120 characters.")
        String accountHolderName,

        @NotNull(message = "Initial deposit is required")
        @PositiveOrZero(message = "Initial deposit cannot be negative.")
        @Digits(integer = 15, fraction = 4, message = "Initial deposit must have at most 15 integer digits and 4 decimal places")
        BigDecimal initialDeposit) { }
