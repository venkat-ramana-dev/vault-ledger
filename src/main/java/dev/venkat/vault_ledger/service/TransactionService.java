package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionEntryRepository transactionEntryRepository;

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
//import dev.venkat.vault_ledger.mapper.TransactionMapper;
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
//            return TransactionMapper.mapToTransactionDto(savedTransaction);
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
//            return TransactionMapper.mapToTransactionDto(transaction);
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
//                .map(TransactionMapper::mapToTransactionDto)
//                .collect(Collectors.toList());
//    }
//}
