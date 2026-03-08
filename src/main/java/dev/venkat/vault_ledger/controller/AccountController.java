package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create")
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto accountDto) {
        Account account = AccountMapper.mapToAccount(accountDto);
        AccountDto savedAccountDto = accountService.createAccount(account);
        return ResponseEntity.ok(savedAccountDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(AccountMapper.mapToAccountDto(account));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAccounts() {
        List<Account> accounts = accountService.getAccounts();
        List<AccountDto> accountDtos = accounts.stream()
                .map(account -> AccountMapper.mapToAccountDto(account))
                .toList();

        return ResponseEntity.ok(accountDtos);
    }
}
