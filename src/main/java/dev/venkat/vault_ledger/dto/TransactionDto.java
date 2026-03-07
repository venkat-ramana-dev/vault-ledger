package dev.venkat.vault_ledger.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionDto(Long id,
                             Long accountId,
                             String type,
                             BigDecimal amount,
                             LocalDateTime createdAt) { }
