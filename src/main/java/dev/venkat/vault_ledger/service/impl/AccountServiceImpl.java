package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.User;

import java.util.List;

public interface AccountServiceImpl {

    AccountDto createAccount(User user, CreateAccountRequestDto createAccountRequestDto);

    AccountDto getAccountDetails(String accountNumber);

    List<AccountDto> getAllAccounts();

    String deleteAccount(String accountNumber);

}
