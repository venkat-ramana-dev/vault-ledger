package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @PutMapping("/{accountNumber}/deposit")
    public ResponseEntity<TransactionDto> deposit(@PathVariable String accountNumber,
                                                  @Valid @RequestBody AmountDto amount) {

        TransactionDto transactionDto = transactionService.deposit(accountNumber, amount);
        return ResponseEntity.ok(transactionDto);
    }

    @PutMapping("/{accountNumber}/withdraw")
    public ResponseEntity<TransactionDto> withdraw(@PathVariable String accountNumber,
                                                   @Valid @RequestBody AmountDto amount) {
        TransactionDto transactionDto = transactionService.withdraw(accountNumber, amount);
        return ResponseEntity.ok(transactionDto);
    }

    @PutMapping("/{fromAccountNumber}/transfer")
    public ResponseEntity<TransferTransactionDto> transfer(@PathVariable String fromAccountNumber,
                                                           @Valid @RequestBody TransferRequestDto transferRequestDto) throws InterruptedException {
        TransferTransactionDto transferTransactionDto = transactionService.transfer(fromAccountNumber, transferRequestDto);
        return ResponseEntity.ok(transferTransactionDto);
    }

}
