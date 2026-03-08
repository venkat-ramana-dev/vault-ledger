package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.Transaction;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionRepository;
import dev.venkat.vault_ledger.service.impl.AccountServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountServiceImpl {

    private final AccountRepository accountRepository;

    //to avoid Circular dependency
    private final TransactionRepository transactionRepository;

    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        Account account = AccountMapper.mapToAccount(accountDto);
        Account savedAccount =  accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .account(savedAccount)
                .type("INITIAL_DEPOSIT")
                .amount(account.getBalance())
                .build();

        transactionRepository.save(transaction);

        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->new RuntimeException("Account not found with id :" + id));

        AccountDto accountDto = AccountMapper.mapToAccountDto(account);
        return accountDto;
    }

    @Override
    public List<AccountDto> getAllAccounts() {

        List<AccountDto> accountDtos = new ArrayList<>();

        accountDtos = accountRepository.findAll().stream()
                .map(account -> AccountMapper.mapToAccountDto(account))
                .toList();

        return accountDtos;
    }

    @Override
    public String deleteAccountById(Long id) {

        Account account = getAccountEntityById(id);
        accountRepository.delete(account);
        return "Account deleted successfully with id" + id;
    }

    @Override
    public Account getAccountEntityById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->new RuntimeException("Account not found with id :" + id));

        return account;
    }
}
