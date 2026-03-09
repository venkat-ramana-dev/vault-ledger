package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;

public interface TransactionServiceImpl {

    TransactionDto deposit(Long id, AmountDto amount);

    TransactionDto withdraw(Long id, AmountDto amount);

}
