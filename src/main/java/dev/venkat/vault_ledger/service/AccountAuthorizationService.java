package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.exception.AccountNotFoundException;
import dev.venkat.vault_ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAuthorizationService {

    private final AccountRepository accountRepository;
    private final SecurityService securityService;

    public Account getOwnedAccount(String accountNumber) {
        String username = securityService.getCurrentUsername();

        return accountRepository
                .findByAccountNumberAndUser_Username(
                        accountNumber,
                        username
                )
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountNumber
                        ));
    }

    public Account getOwnedAccountForUpdate(String accountNumber) {
        String username = securityService.getCurrentUsername();

        return accountRepository
                .findOwnedAccountForUpdate(accountNumber, username)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountNumber
                        ));
    }
}