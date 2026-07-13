package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.enums.TransactionType;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionServiceImpl {

    TransactionDto deposit(String accountNumber, AmountDto amountDto);

    TransactionDto withdraw(String accountNumber, AmountDto amountDto);

    TransferTransactionDto transfer(String accountNumber, TransferRequestDto transferRequestDto) throws InterruptedException;

    BigDecimal getAccountBalance(Long accountId);

    Page<TransactionHistoryDto> getTransactionHistory(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            TransactionType transactionType,
            int page,
            int size,
            String sortBy,
            String sortDir);
    }
