package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.service.impl.AccountServiceImpl;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountServiceImpl {

    private final AccountRepository accountRepository;

    @Override
    public AccountDto createAccount(Account account) {
        Account savedAccount =  accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }


    @Override
    public Account getAccountEntityById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->new RuntimeException("Account not found with id :" + id));

        return account;
    }
}
