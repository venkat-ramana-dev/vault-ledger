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
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import dev.venkat.vault_ledger.service.impl.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

        return TransactionDto.builder()
                .transactionType(TransactionType.DEPOSIT)
                .entryDirection(EntryDirection.DEBIT)
                .amount(amountDto.amount())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    @Override
    public TransactionDto withdraw(String accountNumber, AmountDto amountDto) {

        Account userAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalStateException("System error: Account not found"));

        if(userAccount.getAccountStatus().equals(AccountStatus.CLOSED)){
            throw  new AccountClosedException("Account is closed");
        }
        if (amountDto.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Withdrawal amount cannot be negative");
        }
        if (getAccountBalance(userAccount.getId()).compareTo(amountDto.amount()) < 0){
            throw new InsufficientBalanceException("Insufficient Balance. Cannot withdraw amount :" + amountDto.amount()
                    + "from balance" + getAccountBalance(userAccount.getId()));
        }


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

        return TransactionDto.builder()
                .transactionType(TransactionType.WITHDRAWAl)
                .entryDirection(EntryDirection.CREDIT)
                .amount(amountDto.amount())
                .createdAt(LocalDateTime.now())
                .build();

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

    public List<TransactionDto> getTransactionHistory(String accountNumber) {
        boolean accountExists = accountRepository.findByAccountNumber(accountNumber).isPresent();
        if (!accountExists) {
            throw new IllegalArgumentException("Account not found for number: " + accountNumber);
        }

        List<TransactionEntry> entries = transactionEntryRepository
                .findByAccount_AccountNumberOrderByTransactionHeader_CreatedAtDesc(accountNumber);

        return entries.stream().map(entry -> TransactionDto.builder()
                .transactionType(entry.getTransactionHeader().getTransactionType())
                .entryDirection(entry.getEntryDirection())
                .amount(entry.getAmount())
                .createdAt(entry.getTransactionHeader().getCreatedAt())
                .build()
        ).toList();
    }

    public BigDecimal getAccountBalance(Long accountId) {
        return transactionEntryRepository.calculateBalanceByAccountId(accountId);
    }

}