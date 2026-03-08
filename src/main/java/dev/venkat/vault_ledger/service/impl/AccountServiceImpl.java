package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;

import java.util.List;

public interface AccountServiceImpl {

    AccountDto createAccount(Account account);

    Account getAccountEntityById(Long id);

    Account getAccountById(Long id);

    List<Account> getAccounts();

}
