package dev.venkat.vault_ledger.mapper;

import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.Transaction;

public class TransactionMapper {

    public static Transaction mapToTransaction(TransactionDto transactionDto) {
        return Transaction.builder()
                .id(transactionDto.id())
                .account(transactionDto.account())
                .type(transactionDto.type())
                .amount(transactionDto.amount())
                .createdAt(transactionDto.createdAt())
                .build();
    }

    public static TransactionDto mapToTransactionDto (Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .account(transaction.getAccount())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

}
