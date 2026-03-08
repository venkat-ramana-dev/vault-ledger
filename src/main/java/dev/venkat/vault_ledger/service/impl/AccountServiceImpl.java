package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;

import java.util.List;

public interface AccountServiceImpl {

    AccountDto createAccount(AccountDto accountDto);

    Account getAccountEntityById(Long id);

    AccountDto getAccountById(Long id);

    List<AccountDto> getAllAccounts();

    String deleteAccountById(Long id);

}
