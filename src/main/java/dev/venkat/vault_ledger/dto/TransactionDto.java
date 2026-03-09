package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionDto(Long id,
                             Account account,
                             TransactionType transactionType,
                             BigDecimal amount,
                             LocalDateTime createdAt) { }
