package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TransactionDto(TransactionType transactionType,
                             EntryDirection entryDirection,
                             BigDecimal amount,
                             Instant createdAt) { }
