package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.TransactionHistoryDto;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.service.AccountService;
import dev.venkat.vault_ledger.service.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    private final TransactionService transactionService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountDetails(@PathVariable String accountNumber) {
        AccountDto accountDto = accountService.getAccountDetails(accountNumber);
        return ResponseEntity.ok(accountDto);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accountDtos = accountService.getAllAccounts();
        return ResponseEntity.ok(accountDtos);
    }

    @PatchMapping("/{accountNumber}/close")
    public String closeAccount(@PathVariable String accountNumber) {
        return accountService.closeAccount(accountNumber);
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<Page<TransactionHistoryDto>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Page<TransactionHistoryDto> history = transactionService.getTransactionHistory(
                accountNumber, startDate, endDate, minAmount, maxAmount, transactionType, page, size, sortBy, sortDir
        );

        return ResponseEntity.ok(history);
    }
}
