package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.entity.Account;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionDto(Long id,
                             Account account,
                             String type,
                             BigDecimal amount,
                             LocalDateTime createdAt) { }
