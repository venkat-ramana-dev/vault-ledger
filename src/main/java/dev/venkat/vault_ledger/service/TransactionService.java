package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.Transaction;
import dev.venkat.vault_ledger.repository.TransactionRepository;
import dev.venkat.vault_ledger.service.impl.TransactionServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionServiceImpl{

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Override
    @Transactional
    public Transaction deposit(Long id, AmountDto amountDto) {

        Account account = accountService.getAccountEntityById(id);

        BigDecimal amount = amountDto.amount();

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        Transaction transaction = Transaction.builder()
                .account(account)
                .type("DEPOSIT")
                .amount(amount)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return savedTransaction;

    }
}
