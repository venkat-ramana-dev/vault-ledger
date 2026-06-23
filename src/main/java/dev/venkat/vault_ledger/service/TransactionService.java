package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.dto.TransferRequestDto;
import dev.venkat.vault_ledger.dto.TransferTransactionDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.exception.AccountClosedException;
import dev.venkat.vault_ledger.exception.InsufficientBalanceException;
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
import java.time.LocalDateTime;

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
                        .orElseThrow(() -> new IllegalStateException("System error: Account not found")))
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

    @Transactional
    @Override
    public TransactionDto withdraw(String accountNumber, AmountDto amountDto) {

        Account userAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalStateException("System error: Account not found"));

        if ((amountDto.amount().compareTo(BigDecimal.ZERO)) > 0
                && getAccountBalance(userAccount.getId()).compareTo(amountDto.amount()) > 0
                && userAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {

            TransactionHeader transactionHeader = TransactionHeader.builder()
                    .transactionType(TransactionType.WITHDRAWAl)
                    .build();
            TransactionHeader savedTransactionHeader = transactionHeaderRepository.save(transactionHeader);

            TransactionEntry transactionEntryOfUser = TransactionEntry.builder()
                    .amount(amountDto.amount())
                    .entryDirection(EntryDirection.DEBIT)
                    .account(accountRepository.findByAccountNumber(accountNumber)
                            .orElseThrow(() -> new IllegalStateException("System error: Account not found")))
                    .transactionHeader(savedTransactionHeader)
                    .build();
            TransactionEntry savedTransactionEntryOfUser = transactionEntryRepository.save(transactionEntryOfUser);

            TransactionEntry transactionEntryOfVault = TransactionEntry.builder()
                    .amount(amountDto.amount())
                    .entryDirection(EntryDirection.CREDIT)
                    .account(accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                            .orElseThrow(() -> new IllegalStateException("System error: Vault account not found")))
                    .transactionHeader(savedTransactionHeader)
                    .build();
            TransactionEntry savedTransactionEntryOfVault = transactionEntryRepository.save(transactionEntryOfVault);

            return TransactionEntryMapper.mapToTransactionDto(savedTransactionEntryOfUser);
        } else if(userAccount.getAccountStatus().equals(AccountStatus.CLOSED)){
            throw  new AccountClosedException("Account is closed");
        } else if (amountDto.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Withdrawal amount cannot be negative");
        } else {
            throw new InsufficientBalanceException("Insufficient Balance. Cannot withdraw amount :" + amountDto.amount()
                                                    + "from balance" + getAccountBalance(userAccount.getId()));
        }
    }

    @Transactional
    @Override
    public TransferTransactionDto transfer(String fromAccountNumber, TransferRequestDto transferRequestDto) {

        String toAccountNumber = transferRequestDto.toAccountNumber();
        AmountDto amountDto = new AmountDto(transferRequestDto.amount());

        if (amountDto.amount() == null || amountDto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer money to the same account.");
        }

        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new IllegalStateException("System error: From account not found"));

        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new IllegalStateException("System error: To account not found"));

        if (!fromAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Sender account is not active.");
        }
        if (!toAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Receiver account is not active.");
        }

        BigDecimal fromAccountBalance = getAccountBalance(fromAccount.getId());
        if (fromAccountBalance.compareTo(amountDto.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds for transfer.");
        }


        TransactionHeader transactionHeader = TransactionHeader.builder()
                .transactionType(TransactionType.TRANSFER)
                .build();
        TransactionHeader savedTransactionHeader = transactionHeaderRepository.save(transactionHeader);

        TransactionEntry transactionEntryOfFromAccount = TransactionEntry.builder()
                .amount(amountDto.amount())
                .entryDirection(EntryDirection.DEBIT)
                .account(fromAccount)
                .transactionHeader(savedTransactionHeader)
                .build();
        TransactionEntry savedTransactionEntryOfFromAccount = transactionEntryRepository.save(transactionEntryOfFromAccount);

        TransactionEntry transactionEntryOfToAccount = TransactionEntry.builder()
                .amount(amountDto.amount())
                .entryDirection(EntryDirection.CREDIT)
                .account(toAccount)
                .transactionHeader(savedTransactionHeader)
                .build();
        TransactionEntry savedTransactionEntryOfToAccount = transactionEntryRepository.save(transactionEntryOfToAccount);


        return TransferTransactionDto.builder()
                .fromAccountNumber(fromAccountNumber)
                .fromAccountHolderName(fromAccount.getAccountHolderName())
                .transactionType(TransactionType.TRANSFER)
                .toAccountNumber(toAccountNumber)
                .toAccountHolderName(toAccount.getAccountHolderName())
                .amount(amountDto.amount())
                .createdAt(LocalDateTime.now())
                .build();
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
