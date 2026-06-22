//package dev.venkat.vault_ledger.controller;
//
//import dev.venkat.vault_ledger.dto.AmountDto;
//import dev.venkat.vault_ledger.dto.TransactionDto;
//import dev.venkat.vault_ledger.dto.TransferDto;
//import dev.venkat.vault_ledger.service.TransactionService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping
//@RequiredArgsConstructor
//public class TransactionController {
//
//    private final TransactionService transactionService;
//
//    @PutMapping("/account/{id}/deposit")
//    public ResponseEntity<TransactionDto> deposit(@PathVariable("id") Long id,
//                                                  @RequestBody AmountDto amount) {
//
//        TransactionDto transactionDto = transactionService.deposit(id, amount);
//        return ResponseEntity.ok(transactionDto);
//    }
//
//    @PutMapping("/account/{id}/withdraw")
//    public ResponseEntity<TransactionDto> withdraw(@PathVariable Long id,
//                                                   @RequestBody AmountDto amount) {
//        TransactionDto transactionDto = transactionService.withdraw(id, amount);
//        return ResponseEntity.ok(transactionDto);
//    }
//
//    @PutMapping("/account/{id}/transfer")
//    public ResponseEntity<List<TransactionDto>> transfer(@PathVariable("id") Long fromId,
//                                                         @RequestBody TransferDto transferDto) {
//        List<TransactionDto> transactionDtos = transactionService.transfer(fromId, transferDto);
//        return ResponseEntity.ok(transactionDtos);
//    }
//
//    @GetMapping("/account/{id}/history")
//    public ResponseEntity<List<TransactionDto>> getTransactionHistory(@PathVariable Long id) {
//        return ResponseEntity.ok(transactionService.getTransactionHistory(id));
//    }
//
//
//}
