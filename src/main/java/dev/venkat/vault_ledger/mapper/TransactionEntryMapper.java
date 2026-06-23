package dev.venkat.vault_ledger.mapper;

import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.TransactionEntry;

public class TransactionEntryMapper {

    public static TransactionDto mapToTransactionDto (TransactionEntry transactionEntry) {
        return TransactionDto.builder()
                .accountNumber(transactionEntry.getAccount().getAccountNumber())
                .entryDirection(transactionEntry.getEntryDirection())
                .amount(transactionEntry.getAmount())
                .createdAt(transactionEntry.getCreatedAt())
                .build();
    }

}
