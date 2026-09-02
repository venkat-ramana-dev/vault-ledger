package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.exception.*;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import dev.venkat.vault_ledger.specification.TransactionEntrySpecifications;
import dev.venkat.vault_ledger.util.TransactionDescriptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;

    private final TransactionHeaderRepository transactionHeaderRepository;

    private final TransactionEntryRepository transactionEntryRepository;

    private final AccountAuthorizationService accountAuthorizationService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "amount",
            "createdAt"
    );

    @Transactional
    public TransactionDto deposit(String accountNumber, AmountDto amountDto) {

        log.info("Deposit initiated: Account Number: {}. Amount: {}",
                accountNumber,
                amountDto.amount());

        Account userAccount = accountAuthorizationService.getOwnedAccount(accountNumber);

        Account vaultAccount = accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER));

        if(userAccount.getAccountStatus() == AccountStatus.CLOSED){
            throw  new AccountClosedException("Account is closed: " + accountNumber);
        }

        TransactionHeader savedTransactionHeader = createDoubleEntryTransaction(
                TransactionType.DEPOSIT,
                amountDto.amount(),
                vaultAccount,
                TransactionDescriptionUtil.systemVaultWithdrawal(userAccount),
                userAccount,
                TransactionDescriptionUtil.cashDeposit());

        log.info("Deposit completed. Account Number: {}. Amount: {}",
                accountNumber,
                amountDto.amount());
        return TransactionDto.builder()
                .transactionType(TransactionType.DEPOSIT)
                .entryDirection(EntryDirection.CREDIT)
                .amount(amountDto.amount())
                .createdAt(savedTransactionHeader.getCreatedAt())
                .build();
    }

    @Transactional
    public TransactionDto withdraw(String accountNumber, AmountDto amountDto) {

        log.info("Withdrawal initiated. Account Number: {}. Amount: {}",
                accountNumber,
                amountDto.amount());

        Account userAccount = accountAuthorizationService.getOwnedAccountForUpdate(accountNumber);

        Account vaultAccount = accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER));

        if(userAccount.getAccountStatus() == AccountStatus.CLOSED){
            throw  new AccountClosedException("Account is closed: " + accountNumber);
        }

        BigDecimal balance = getAccountBalance(userAccount.getId());

        if (balance.compareTo(amountDto.amount()) < 0){
            throw new InsufficientBalanceException("Insufficient Balance. Cannot withdraw amount: " + amountDto.amount()
                    + " from balance: " + balance);
        }

        TransactionHeader savedTransactionHeader = createDoubleEntryTransaction(
                TransactionType.WITHDRAWAL,
                amountDto.amount(),
                userAccount,
                TransactionDescriptionUtil.cashWithdrawal(),
                vaultAccount,
                TransactionDescriptionUtil.systemVaultDeposit(userAccount));


        log.info("Withdrawal completed. Account Number: {}. Amount: {}",
                accountNumber,
                amountDto.amount());
        return TransactionDto.builder()
                .transactionType(TransactionType.WITHDRAWAL)
                .entryDirection(EntryDirection.DEBIT)
                .amount(amountDto.amount())
                .createdAt(savedTransactionHeader.getCreatedAt())
                .build();

    }

    @Transactional
    public TransferTransactionDto transfer(String fromAccountNumber, TransferRequestDto transferRequestDto) {

        log.info("Transfer initiated. From Account Number: {}. To Account Number: {}. Amount: {}",
                fromAccountNumber,
                transferRequestDto.toAccountNumber(),
                transferRequestDto.amount());
        String toAccountNumber = transferRequestDto.toAccountNumber();
        AmountDto amountDto = new AmountDto(transferRequestDto.amount());

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new SameAccountTransferException("Cannot transfer money to the same account: " + toAccountNumber);
        }

        Account fromAccount;
        Account toAccount;

        if (fromAccountNumber.compareTo(toAccountNumber) < 0) {
            fromAccount = accountAuthorizationService.getOwnedAccountForUpdate(fromAccountNumber);
            toAccount = accountRepository.findByAccountNumberForUpdate(toAccountNumber)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + toAccountNumber));
        } else {
            toAccount = accountRepository.findByAccountNumberForUpdate(toAccountNumber)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + toAccountNumber));
            fromAccount = accountAuthorizationService.getOwnedAccountForUpdate(fromAccountNumber);
        }

        if (fromAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account is closed: " + fromAccountNumber);
        }
        if (toAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account is closed: " + toAccountNumber);
        }

        BigDecimal fromAccountBalance = getAccountBalance(fromAccount.getId());
        if (fromAccountBalance.compareTo(amountDto.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient funds for transfer." +
                                            "Amount: " + amountDto.amount() +
                                            " Balance: " + fromAccountBalance);
        }

        TransactionHeader savedTransactionHeader = createDoubleEntryTransaction(
                TransactionType.TRANSFER,
                amountDto.amount(),
                fromAccount,
                TransactionDescriptionUtil.transferTo(toAccount),
                toAccount,
                TransactionDescriptionUtil.transferFrom(fromAccount));

        log.info("Transfer completed successfully. Header {}. Sender: {}.Receiver: {}.Amount: {}",
                savedTransactionHeader.getId(),
                fromAccount.getAccountHolderName(),
                toAccount.getAccountHolderName(),
                amountDto.amount());

        return TransferTransactionDto.builder()
                .fromAccountNumber(fromAccountNumber)
                .fromAccountHolderName(fromAccount.getAccountHolderName())
                .transactionType(TransactionType.TRANSFER)
                .toAccountNumber(toAccountNumber)
                .toAccountHolderName(toAccount.getAccountHolderName())
                .amount(amountDto.amount())
                .createdAt(savedTransactionHeader.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        return transactionEntryRepository.calculateBalanceByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceProjection> getAccountBalances(List<Long> accountIds) {
        return transactionEntryRepository.calculateBalancesForAccounts(accountIds);
    }

    @Transactional
    public TransactionHeader createDoubleEntryTransaction(
            TransactionType transactionType,
            BigDecimal amount,
            Account debitAccount,
            String debitDescription,
            Account creditAccount,
            String creditDescription
    ) {
        Instant createdAt = Instant.now();

        TransactionHeader transactionHeader = TransactionHeader.builder()
                .transactionType(transactionType)
                .createdAt(createdAt)
                .build();

        TransactionHeader savedHeader =
                transactionHeaderRepository.save(transactionHeader);

        TransactionEntry debitEntry = TransactionEntry.builder()
                .amount(amount)
                .entryDirection(EntryDirection.DEBIT)
                .account(debitAccount)
                .transactionHeader(savedHeader)
                .description(debitDescription)
                .createdAt(createdAt)
                .build();

        TransactionEntry creditEntry = TransactionEntry.builder()
                .amount(amount)
                .entryDirection(EntryDirection.CREDIT)
                .account(creditAccount)
                .transactionHeader(savedHeader)
                .description(creditDescription)
                .createdAt(createdAt)
                .build();

        transactionEntryRepository.save(debitEntry);
        transactionEntryRepository.save(creditEntry);

        return savedHeader;
    }

    @Transactional(readOnly = true)
    public Page<TransactionHistoryDto> getTransactionHistory(
            String accountNumber,
            Instant startDate,
            Instant endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            TransactionType transactionType,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Account userAccount = accountAuthorizationService.getOwnedAccount(accountNumber);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "sortDir must be either ASC or DESC.");
        }

        Sort sort = Sort.by(direction, sortBy);

        if (page < 0) {
            throw new InvalidPageRangeException(
                  "Page cannot be negative"
            );
        } else if (size <= 0 || size > 100) {
            throw new InvalidSizeRangeException(
                    "Size can be only from 1 to 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        if (minAmount != null && maxAmount != null
                && minAmount.compareTo(maxAmount) > 0) {
            throw new InvalidAmountRangeException(
                    "Minimum amount cannot be greater than maximum amount.");
        }

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(
                    "Start date cannot be after end date.");
        }

        Specification<TransactionEntry> spec = Specification.allOf(
                TransactionEntrySpecifications.belongsToAccount(userAccount),
                TransactionEntrySpecifications.dateBetween(startDate, endDate),
                TransactionEntrySpecifications.amountBetween(minAmount, maxAmount),
                TransactionEntrySpecifications.transactionType(transactionType)
        );

        Page<TransactionEntry> entries = transactionEntryRepository.findAll(spec, pageable);

        return entries.map(entry -> TransactionHistoryDto.builder()
                .transactionType(entry.getTransactionHeader().getTransactionType())
                .entryDirection(entry.getEntryDirection())
                .amount(entry.getAmount())
                .description(entry.getDescription())
                .createdAt(entry.getTransactionHeader().getCreatedAt())
                .build()
        );
    }

}