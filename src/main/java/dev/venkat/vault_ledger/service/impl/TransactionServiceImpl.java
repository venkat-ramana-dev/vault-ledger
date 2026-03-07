package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.Transaction;

import java.math.BigDecimal;

public interface TransactionServiceImpl {

    Transaction deposit(Long id, AmountDto amount);

}
