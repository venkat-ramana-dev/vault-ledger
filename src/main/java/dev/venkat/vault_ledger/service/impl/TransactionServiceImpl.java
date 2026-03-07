package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.Transaction;

public interface TransactionServiceImpl {

    Transaction deposit(TransactionDto transactionDto);

}
