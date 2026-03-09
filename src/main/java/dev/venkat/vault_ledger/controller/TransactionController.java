package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.Transaction;
import dev.venkat.vault_ledger.mapper.TransactionMapper;
import dev.venkat.vault_ledger.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PutMapping("/account/{id}/deposit")
    public ResponseEntity<TransactionDto> deposit(@PathVariable("id") Long id,
                                                  @RequestBody AmountDto amount) {

        TransactionDto transactionDto = transactionService.deposit(id, amount);
        return ResponseEntity.ok(transactionDto);
    }

    @PutMapping("/account/{id}/withdraw")
    public ResponseEntity<TransactionDto> withdraw(@PathVariable Long id,
                                                   @RequestBody AmountDto amount) {
        TransactionDto transactionDto = transactionService.withdraw(id, amount);
        return ResponseEntity.ok(transactionDto);
    }


}
