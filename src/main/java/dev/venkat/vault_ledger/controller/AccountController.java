package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
