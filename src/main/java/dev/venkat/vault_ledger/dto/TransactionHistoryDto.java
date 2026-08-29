package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TransactionHistoryDto(TransactionType transactionType,
                                    EntryDirection entryDirection,
                                    BigDecimal amount,
                                    String description,
                                    Instant createdAt) { }
