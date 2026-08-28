package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TransferTransactionDto(String fromAccountNumber,
                                     String fromAccountHolderName,
                                     TransactionType transactionType,
                                     String toAccountNumber,
                                     String toAccountHolderName,
                                     BigDecimal amount,
                                     Instant createdAt) {
}
