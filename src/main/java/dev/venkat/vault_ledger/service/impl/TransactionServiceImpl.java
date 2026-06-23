package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.dto.TransferRequestDto;
import dev.venkat.vault_ledger.dto.TransferTransactionDto;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionServiceImpl {

    TransactionDto deposit(String accountNumber, AmountDto amountDto);

    TransactionDto withdraw(String accountNumber, AmountDto amountDto);

    TransferTransactionDto transfer(String accountNumber, TransferRequestDto transferRequestDto);

    BigDecimal getAccountBalance(Long accountId);

//    TransactionDto deposit(Long id, AmountDto amount);
//
//    TransactionDto withdraw(Long id, AmountDto amount);
//
//    List<TransactionDto> transfer(Long fromId, TransferDto transferDto);
//
//    List<TransactionDto> getTransactionHistory(Long id);

}
