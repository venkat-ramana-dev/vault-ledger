package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
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
    public ResponseEntity<AccountDto> createAccount(@RequestBody CreateAccountRequestDto createAccountRequestDto) {

        AccountDto savedAccountDto = accountService.createAccount(createAccountRequestDto);
        return ResponseEntity.ok(savedAccountDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto accountDto = accountService.getAccountById(id);
        return ResponseEntity.ok(accountDto);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accountDtos = accountService.getAllAccounts();

        return ResponseEntity.ok(accountDtos);
    }

    @DeleteMapping("/{id}/delete")
    public String deleteAccountById(@PathVariable Long id) {
        return accountService.deleteAccountById(id);
    }
}
