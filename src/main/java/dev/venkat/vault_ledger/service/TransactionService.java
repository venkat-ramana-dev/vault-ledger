package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.mapper.TransactionEntryMapper;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import dev.venkat.vault_ledger.service.impl.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionServiceImpl {

    private final AccountRepository accountRepository;

    private final TransactionHeaderRepository transactionHeaderRepository;

    private final TransactionEntryRepository transactionEntryRepository;

    @Transactional
    @Override
    public TransactionDto deposit(String accountNumber, AmountDto amountDto) {
        TransactionHeader transactionHeader = TransactionHeader.builder()
                .transactionType(TransactionType.DEPOSIT)
                .build();
        TransactionHeader savedTransactionHeader = transactionHeaderRepository.save(transactionHeader);

        TransactionEntry transactionEntryOfUser = TransactionEntry.builder()
                .amount(amountDto.amount())
                .entryDirection(EntryDirection.CREDIT)
                .account(accountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new IllegalStateException("System error: Vault account not found")))
                .transactionHeader(savedTransactionHeader)
                .build();
        TransactionEntry savedTransactionEntryOfUser = transactionEntryRepository.save(transactionEntryOfUser);

        TransactionEntry transactionEntryOfVault = TransactionEntry.builder()
                .amount(amountDto.amount())
                .entryDirection(EntryDirection.DEBIT)
                .account(accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                        .orElseThrow(() -> new IllegalStateException("System error: Vault account not found")))
                .transactionHeader(savedTransactionHeader)
                .build();
        TransactionEntry savedTransactionEntryOfVault = transactionEntryRepository.save(transactionEntryOfVault);

        return TransactionEntryMapper.mapToTransactionDto(savedTransactionEntryOfUser);
    }

    public BigDecimal getAccountBalance(Long accountId) {
        return transactionEntryRepository.calculateBalanceByAccountId(accountId);
    }

}



























//
//import dev.venkat.vault_ledger.dto.AmountDto;
//import dev.venkat.vault_ledger.dto.TransactionDto;
//import dev.venkat.vault_ledger.dto.TransferDto;
//import dev.venkat.vault_ledger.entity.Account;
//import dev.venkat.vault_ledger.entity.Transaction;
//import dev.venkat.vault_ledger.enums.AccountStatus;
//import dev.venkat.vault_ledger.enums.TransactionType;
//import dev.venkat.vault_ledger.exception.AccountClosedException;
//import dev.venkat.vault_ledger.exception.InsufficientBalanceException;
//import dev.venkat.vault_ledger.mapper.TransactionEntryMapper;
//import dev.venkat.vault_ledger.repository.TransactionRepository;
//import dev.venkat.vault_ledger.service.impl.TransactionServiceImpl;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collector;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class TransactionService implements TransactionServiceImpl{
//
//    private final TransactionRepository transactionRepository;
//    private final AccountService accountService;
//
//    @Override
//    @Transactional
//    public TransactionDto deposit(Long id, AmountDto amountDto) {
//
//        Account account = accountService.getAccountEntityById(id);
//
//        if (account.getAccountStatus().equals(AccountStatus.ACTIVE)) {
//            BigDecimal amount = amountDto.amount();
//
//            BigDecimal newBalance = account.getBalance().add(amount);
//            account.setBalance(newBalance);
//
//            Transaction transaction = Transaction.builder()
//                    .account(account)
//                    .transactionType(TransactionType.DEPOSIT)
//                    .amount(amount)
//                    .build();
//
//            Transaction savedTransaction = transactionRepository.save(transaction);
//
//            return TransactionEntryMapper.mapToTransactionDto(savedTransaction);
//        } else {
//            throw new AccountClosedException("Account is Closed with id " + id);
//        }
//
//
//    }
//
//    @Override
//    @Transactional
//    public TransactionDto withdraw(Long id, AmountDto amount) {
//
//        Account account = accountService.getAccountEntityById(id);
//
//        if (account.getBalance().compareTo(amount.amount()) >= 0 && account.getAccountStatus().equals(AccountStatus.ACTIVE)) {
//            account.setBalance(account.getBalance().subtract(amount.amount()));
//
//            Transaction transaction = Transaction.builder()
//                    .account(account)
//                    .transactionType(TransactionType.WITHDRAWAl)
//                    .amount(amount.amount())
//                    .build();
//
//            transactionRepository.save(transaction);
//
//            return TransactionEntryMapper.mapToTransactionDto(transaction);
//        } else if (!account.getAccountStatus().equals(AccountStatus.ACTIVE)) {
//            throw new AccountClosedException("Account is closed");
//        } else {
//            throw new InsufficientBalanceException("Insufficient balance. Cannot withdraw " + amount.amount());
//        }
//    }
//
//    @Override
//    @Transactional
//    public List<TransactionDto> transfer(Long fromId, TransferDto transferDto) {
//
//        List<TransactionDto> transactionDtos = new ArrayList<>();
//
//        Long toId = transferDto.toId();
//        AmountDto amount = new AmountDto(transferDto.amount());
//
//        TransactionDto fromTransactionDto = withdraw(fromId, amount);
//        TransactionDto toTransactionDto = deposit(toId, amount);
//
//        transactionDtos.add(fromTransactionDto);
//        transactionDtos.add(toTransactionDto);
//
//        return transactionDtos;
//    }
//
//    @Override
//    public List<TransactionDto> getTransactionHistory(Long id) {
//
//        List<Transaction> transactions = transactionRepository.findByAccountId(id);
//
//        return transactions.stream()
//                .map(TransactionEntryMapper::mapToTransactionDto)
//                .collect(Collectors.toList());
//    }
//}
