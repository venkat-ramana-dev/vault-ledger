package dev.venkat.vault_ledger.mapper;

import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.Transaction;
import dev.venkat.vault_ledger.enums.TransactionType;

public class TransactionMapper {

    public static Transaction mapToTransaction(TransactionDto transactionDto) {
        return Transaction.builder()
                .id(transactionDto.id())
                .account(transactionDto.account())
                .transactionType(transactionDto.transactionType())
                .amount(transactionDto.amount())
                .createdAt(transactionDto.createdAt())
                .build();
    }

    public static TransactionDto mapToTransactionDto (Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .account(transaction.getAccount())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

}
