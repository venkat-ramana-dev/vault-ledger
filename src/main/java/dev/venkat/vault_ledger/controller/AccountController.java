package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountDetails(@PathVariable String accountNumber) {
        AccountDto accountDto = accountService.getAccountDetails(accountNumber);
        return ResponseEntity.ok(accountDto);
    }

    // Future JWT Rule: Admin Only!
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accountDtos = accountService.getAllAccounts();
        return ResponseEntity.ok(accountDtos);
    }

    @DeleteMapping("/{accountNumber}/delete")
    public String deleteAccount(@PathVariable String accountNumber) {
        return accountService.deleteAccount(accountNumber);
    }
}
