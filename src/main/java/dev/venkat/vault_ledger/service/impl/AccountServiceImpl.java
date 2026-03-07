package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;

public interface AccountServiceImpl {

    AccountDto createAccount(Account account);

    Account getAccountEntityById(Long id);

}
