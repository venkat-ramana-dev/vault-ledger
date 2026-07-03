package dev.venkat.vault_ledger.dto;

import java.math.BigDecimal;

public record AuthRequestDto(String username,
                             String password,
                             String accountHolderName,
                             BigDecimal initialDeposit) { }
