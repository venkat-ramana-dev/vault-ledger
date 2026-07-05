package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;

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

        @PositiveOrZero(message = "Initial deposit cannot be negative.")
        BigDecimal initialDeposit) { }
