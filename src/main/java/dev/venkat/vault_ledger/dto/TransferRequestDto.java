package dev.venkat.vault_ledger.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransferRequestDto(String toAccountNumber,
                                 BigDecimal amount) { }
