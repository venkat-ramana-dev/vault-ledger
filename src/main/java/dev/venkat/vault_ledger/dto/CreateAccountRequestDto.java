package dev.venkat.vault_ledger.dto;

import java.math.BigDecimal;

public record CreateAccountRequestDto(String accountHolderName,
                                      BigDecimal initialDeposit) { }