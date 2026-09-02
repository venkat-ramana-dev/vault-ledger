package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.dto.TransactionHistoryDto;
import dev.venkat.vault_ledger.dto.TransferRequestDto;
import dev.venkat.vault_ledger.dto.TransferTransactionDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.exception.AccountClosedException;
import dev.venkat.vault_ledger.exception.AccountNotFoundException;
import dev.venkat.vault_ledger.exception.InsufficientBalanceException;
import dev.venkat.vault_ledger.exception.InvalidAmountRangeException;
import dev.venkat.vault_ledger.exception.InvalidDateRangeException;
import dev.venkat.vault_ledger.exception.InvalidPageRangeException;
import dev.venkat.vault_ledger.exception.InvalidSizeRangeException;
import dev.venkat.vault_ledger.exception.SameAccountTransferException;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionHeaderRepository transactionHeaderRepository;

    @Mock
    private TransactionEntryRepository transactionEntryRepository;

    @Mock
    private AccountAuthorizationService accountAuthorizationService;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<TransactionHeader> transactionHeaderCaptor;

    @Captor
    private ArgumentCaptor<TransactionEntry> transactionEntryCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private String accountNumber;
    private String toAccountNumber;
    private Account userAccount;
    private Account toAccount;
    private Account vaultAccount;

    @BeforeEach
    void setUp() {
        accountNumber = "ACC1234567890";
        toAccountNumber = "ACC9876543210";

        userAccount = Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .accountHolderName("Venkat Ramana")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        toAccount = Account.builder()
                .id(2L)
                .accountNumber(toAccountNumber)
                .accountHolderName("Receiver")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        vaultAccount = Account.builder()
                .id(3L)
                .accountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                .accountHolderName("System Vault")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }


    @Nested
    class DepositTests {

        @Test
        void shouldCreateDepositWhenAccountIsActive() {

            // Arrange
            AmountDto amountDto = new AmountDto(
                    new BigDecimal("1000.00")
            );

            TransactionHeader transactionHeader = TransactionHeader.builder()
                    .id(1L)
                    .transactionType(TransactionType.DEPOSIT)
                    .createdAt(Instant.now())
                    .build();

            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            /*
             * createDoubleEntryTransaction() is part of the same service.
             * For this test, mock the method's result so that the deposit
             * test focuses on deposit-specific logic rather than testing
             * double-entry creation again.
             */
            TransactionService serviceSpy = spy(transactionService);

            doReturn(transactionHeader)
                    .when(serviceSpy)
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );

            // Act
            TransactionDto result =
                    serviceSpy.deposit(accountNumber, amountDto);

            // Assert
            assertEquals(TransactionType.DEPOSIT, result.transactionType());
            assertEquals(EntryDirection.CREDIT, result.entryDirection());
            assertEquals(amountDto.amount(), result.amount());
            assertEquals(
                    transactionHeader.getCreatedAt(),
                    result.createdAt()
            );

            verify(accountAuthorizationService)
                    .getOwnedAccount(accountNumber);

            verify(accountRepository)
                    .findByAccountNumber(
                            VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER
                    );

            verify(serviceSpy).createDoubleEntryTransaction(
                    eq(TransactionType.DEPOSIT),
                    eq(amountDto.amount()),
                    eq(vaultAccount),
                    anyString(),
                    eq(userAccount),
                    anyString()
            );
        }


        @Test
        void shouldThrowExceptionWhenDepositAccountDoesNotExist() {

            // Arrange
            AmountDto amountDto = new AmountDto(
                    new BigDecimal("1000.00")
            );

            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenThrow(new AccountNotFoundException(
                            "Account not found: " + accountNumber));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> serviceSpy.deposit(accountNumber, amountDto)
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(accountAuthorizationService)
                    .getOwnedAccount(accountNumber);

            // Critical: Processing must stop when the user's
            // account cannot be found.
            verify(accountRepository, never())
                    .findByAccountNumber(
                            VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER
                    );

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenDepositAccountIsClosed() {

            // Arrange
            userAccount.setAccountStatus(AccountStatus.CLOSED);

            AmountDto amountDto = new AmountDto(
                    new BigDecimal("1000.00")
            );

            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountClosedException exception = assertThrows(
                    AccountClosedException.class,
                    () -> serviceSpy.deposit(accountNumber, amountDto)
            );

            // Assert
            assertEquals(
                    "Account is closed: " + accountNumber,
                    exception.getMessage()
            );

            // Critical: No transaction should be created for
            // a closed account.
            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }
    }


    @Nested
    class WithdrawTests {

        @Test
        void shouldCreateWithdrawalWhenBalanceIsSufficient() {

            // Arrange
            AmountDto amountDto = new AmountDto(
                    new BigDecimal("500.00")
            );

            TransactionHeader transactionHeader =
                    TransactionHeader.builder()
                            .id(1L)
                            .transactionType(TransactionType.WITHDRAWAL)
                            .createdAt(Instant.now())
                            .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            when(transactionEntryRepository.calculateBalanceByAccountId(
                    userAccount.getId()))
                    .thenReturn(new BigDecimal("1000.00"));

            TransactionService serviceSpy = spy(transactionService);

            doReturn(transactionHeader)
                    .when(serviceSpy)
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );

            // Act
            TransactionDto result =
                    serviceSpy.withdraw(accountNumber, amountDto);

            // Assert
            assertEquals(
                    TransactionType.WITHDRAWAL,
                    result.transactionType()
            );

            assertEquals(
                    EntryDirection.DEBIT,
                    result.entryDirection()
            );

            assertEquals(
                    amountDto.amount(),
                    result.amount()
            );

            verify(transactionEntryRepository)
                    .calculateBalanceByAccountId(userAccount.getId());

            verify(serviceSpy).createDoubleEntryTransaction(
                    eq(TransactionType.WITHDRAWAL),
                    eq(amountDto.amount()),
                    eq(userAccount),
                    anyString(),
                    eq(vaultAccount),
                    anyString()
            );
        }


        @Test
        void shouldThrowExceptionWhenWithdrawalAccountDoesNotExist() {

            // Arrange
            AmountDto amountDto = new AmountDto(
                    new BigDecimal("500.00")
            );

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenThrow(new AccountNotFoundException(
                            "Account not found: " + accountNumber));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> serviceSpy.withdraw(accountNumber, amountDto)
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenWithdrawalAccountIsClosed() {

            // Arrange
            userAccount.setAccountStatus(AccountStatus.CLOSED);

            AmountDto amountDto = new AmountDto(
                    new BigDecimal("500.00")
            );

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountClosedException exception = assertThrows(
                    AccountClosedException.class,
                    () -> serviceSpy.withdraw(accountNumber, amountDto)
            );

            // Assert
            assertEquals(
                    "Account is closed: " + accountNumber,
                    exception.getMessage()
            );

            // Critical: Balance and transaction creation should
            // not happen for a closed account.
            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenWithdrawalBalanceIsInsufficient() {

            // Arrange
            AmountDto amountDto = new AmountDto(
                    new BigDecimal("1000.00")
            );

            BigDecimal balance = new BigDecimal("500.00");

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            when(transactionEntryRepository.calculateBalanceByAccountId(
                    userAccount.getId()))
                    .thenReturn(balance);

            TransactionService serviceSpy = spy(transactionService);

            // Act
            InsufficientBalanceException exception = assertThrows(
                    InsufficientBalanceException.class,
                    () -> serviceSpy.withdraw(accountNumber, amountDto)
            );

            // Assert
            assertEquals(
                    "Insufficient Balance. Cannot withdraw amount: "
                            + amountDto.amount()
                            + " from balance: "
                            + balance,
                    exception.getMessage()
            );

            verify(transactionEntryRepository)
                    .calculateBalanceByAccountId(userAccount.getId());

            // Critical: No transaction should be created when
            // the account has insufficient balance.
            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }
    }


    @Nested
    class TransferTests {

        @Test
        void shouldCreateTransferWhenAccountsAreValidAndBalanceIsSufficient() {

            // Arrange
            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            TransactionHeader transactionHeader =
                    TransactionHeader.builder()
                            .id(1L)
                            .transactionType(TransactionType.TRANSFER)
                            .createdAt(Instant.now())
                            .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumberForUpdate(toAccountNumber))
                    .thenReturn(Optional.of(toAccount));

            when(transactionEntryRepository.calculateBalanceByAccountId(
                    userAccount.getId()))
                    .thenReturn(new BigDecimal("1000.00"));

            TransactionService serviceSpy = spy(transactionService);

            doReturn(transactionHeader)
                    .when(serviceSpy)
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );

            // Act
            TransferTransactionDto result =
                    serviceSpy.transfer(accountNumber, request);

            // Assert
            assertEquals(
                    accountNumber,
                    result.fromAccountNumber()
            );

            assertEquals(
                    toAccountNumber,
                    result.toAccountNumber()
            );

            assertEquals(
                    userAccount.getAccountHolderName(),
                    result.fromAccountHolderName()
            );

            assertEquals(
                    toAccount.getAccountHolderName(),
                    result.toAccountHolderName()
            );

            assertEquals(
                    TransactionType.TRANSFER,
                    result.transactionType()
            );

            assertEquals(
                    request.amount(),
                    result.amount()
            );

            verify(transactionEntryRepository)
                    .calculateBalanceByAccountId(userAccount.getId());

            verify(serviceSpy).createDoubleEntryTransaction(
                    eq(TransactionType.TRANSFER),
                    eq(request.amount()),
                    eq(userAccount),
                    anyString(),
                    eq(toAccount),
                    anyString()
            );
        }


        @Test
        void shouldThrowExceptionWhenTransferringToSameAccount() {

            // Arrange
            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(accountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            TransactionService serviceSpy = spy(transactionService);

            // Act
            SameAccountTransferException exception = assertThrows(
                    SameAccountTransferException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Cannot transfer money to the same account: "
                            + accountNumber,
                    exception.getMessage()
            );

            // Critical: Account lookup, balance calculation and
            // transaction creation must not happen.
            verify(accountRepository, never())
                    .findByAccountNumberForUpdate(anyString());

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(accountAuthorizationService, never())
                    .getOwnedAccountForUpdate(anyString());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenFromAccountDoesNotExist() {

            // Arrange
            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenThrow(new AccountNotFoundException(
                            "Account not found: " + accountNumber));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(accountAuthorizationService)
                    .getOwnedAccountForUpdate(accountNumber);

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenToAccountDoesNotExist() {

            // Arrange
            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumberForUpdate(toAccountNumber))
                    .thenReturn(Optional.empty());

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Account not found: " + toAccountNumber,
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenFromAccountIsClosed() {

            // Arrange
            userAccount.setAccountStatus(AccountStatus.CLOSED);

            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumberForUpdate(toAccountNumber))
                    .thenReturn(Optional.of(toAccount));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountClosedException exception = assertThrows(
                    AccountClosedException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Account is closed: " + accountNumber,
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenToAccountIsClosed() {

            // Arrange
            toAccount.setAccountStatus(AccountStatus.CLOSED);

            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("500.00"))
                    .build();

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumberForUpdate(toAccountNumber))
                    .thenReturn(Optional.of(toAccount));

            TransactionService serviceSpy = spy(transactionService);

            // Act
            AccountClosedException exception = assertThrows(
                    AccountClosedException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Account is closed: " + toAccountNumber,
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .calculateBalanceByAccountId(anyLong());

            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldThrowExceptionWhenTransferBalanceIsInsufficient() {

            // Arrange
            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(toAccountNumber)
                    .amount(new BigDecimal("1000.00"))
                    .build();

            BigDecimal balance = new BigDecimal("500.00");

            when(accountAuthorizationService.getOwnedAccountForUpdate(accountNumber))
                    .thenReturn(userAccount);

            when(accountRepository.findByAccountNumberForUpdate(toAccountNumber))
                    .thenReturn(Optional.of(toAccount));

            when(transactionEntryRepository.calculateBalanceByAccountId(
                    userAccount.getId()))
                    .thenReturn(balance);

            TransactionService serviceSpy = spy(transactionService);

            // Act
            InsufficientBalanceException exception = assertThrows(
                    InsufficientBalanceException.class,
                    () -> serviceSpy.transfer(accountNumber, request)
            );

            // Assert
            assertEquals(
                    "Insufficient funds for transfer.Amount: "
                            + request.amount()
                            + " Balance: "
                            + balance,
                    exception.getMessage()
            );

            verify(transactionEntryRepository)
                    .calculateBalanceByAccountId(userAccount.getId());

            // Critical: No transaction should be created when
            // the sender has insufficient funds.
            verify(serviceSpy, never())
                    .createDoubleEntryTransaction(
                            any(),
                            any(),
                            any(),
                            anyString(),
                            any(),
                            anyString()
                    );
        }


        @Test
        void shouldLockAccountsInConsistentOrderWhenFromAccountNumberIsGreater() {

            // Arrange
            String smallerAccountNumber = "ACC1000000000";
            String greaterAccountNumber = "ACC9000000000";

            Account fromAccount = Account.builder()
                    .id(1L)
                    .accountNumber(greaterAccountNumber)
                    .accountHolderName("Sender")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            Account receiverAccount = Account.builder()
                    .id(2L)
                    .accountNumber(smallerAccountNumber)
                    .accountHolderName("Receiver")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            TransferRequestDto request = TransferRequestDto.builder()
                    .toAccountNumber(smallerAccountNumber)
                    .amount(new BigDecimal("100.00"))
                    .build();

            when(accountRepository.findByAccountNumberForUpdate(smallerAccountNumber))
                    .thenReturn(Optional.of(receiverAccount));

            when(accountAuthorizationService.getOwnedAccountForUpdate(greaterAccountNumber))
                    .thenReturn(fromAccount);

            when(transactionEntryRepository.calculateBalanceByAccountId(
                    fromAccount.getId()))
                    .thenReturn(new BigDecimal("500.00"));

            TransactionHeader transactionHeader =
                    TransactionHeader.builder()
                            .id(1L)
                            .transactionType(TransactionType.TRANSFER)
                            .createdAt(Instant.now())
                            .build();

            TransactionService serviceSpy = spy(transactionService);

            doReturn(transactionHeader)
                    .when(serviceSpy)
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );

            // Act
            serviceSpy.transfer(greaterAccountNumber, request);

            // Assert
            var inOrder = inOrder(
                    accountRepository,
                    accountAuthorizationService
            );

            inOrder.verify(accountRepository)
                    .findByAccountNumberForUpdate(smallerAccountNumber);

            inOrder.verify(accountAuthorizationService)
                    .getOwnedAccountForUpdate(greaterAccountNumber);
        }
    }


    @Nested
    class GetAccountBalanceTests {

        @Test
        void shouldReturnAccountBalance() {

            // Arrange
            BigDecimal balance = new BigDecimal("2500.00");

            when(transactionEntryRepository.calculateBalanceByAccountId(1L))
                    .thenReturn(balance);

            // Act
            BigDecimal result =
                    transactionService.getAccountBalance(1L);

            // Assert
            assertEquals(balance, result);

            verify(transactionEntryRepository)
                    .calculateBalanceByAccountId(1L);
        }
    }


    @Nested
    class GetAccountBalancesTests {

        @Test
        void shouldReturnBalancesForAccounts() {

            // Arrange
            List<Long> accountIds = List.of(1L, 2L);

            AccountBalanceProjection firstBalance =
                    mock(AccountBalanceProjection.class);

            AccountBalanceProjection secondBalance =
                    mock(AccountBalanceProjection.class);

            when(transactionEntryRepository.calculateBalancesForAccounts(accountIds))
                    .thenReturn(List.of(firstBalance, secondBalance));

            // Act
            List<AccountBalanceProjection> result =
                    transactionService.getAccountBalances(accountIds);

            // Assert
            assertEquals(
                    List.of(firstBalance, secondBalance),
                    result
            );

            verify(transactionEntryRepository)
                    .calculateBalancesForAccounts(accountIds);
        }
    }


    @Nested
    class CreateDoubleEntryTransactionTests {

        @Test
        void shouldCreateTransactionHeaderAndTwoEntries() {

            // Arrange
            BigDecimal amount = new BigDecimal("1000.00");

            TransactionHeader savedHeader =
                    TransactionHeader.builder()
                            .id(10L)
                            .transactionType(TransactionType.DEPOSIT)
                            .createdAt(Instant.now())
                            .build();

            when(transactionHeaderRepository.save(any(TransactionHeader.class)))
                    .thenReturn(savedHeader);

            // Act
            TransactionHeader result =
                    transactionService.createDoubleEntryTransaction(
                            TransactionType.DEPOSIT,
                            amount,
                            vaultAccount,
                            "Vault withdrawal",
                            userAccount,
                            "Cash deposit"
                    );

            // Assert
            assertSame(savedHeader, result);

            verify(transactionHeaderRepository)
                    .save(transactionHeaderCaptor.capture());

            TransactionHeader savedHeaderArgument =
                    transactionHeaderCaptor.getValue();

            assertEquals(
                    TransactionType.DEPOSIT,
                    savedHeaderArgument.getTransactionType()
            );

            assertNotNull(savedHeaderArgument.getCreatedAt());

            verify(transactionEntryRepository, times(2))
                    .save(transactionEntryCaptor.capture());

            List<TransactionEntry> entries =
                    transactionEntryCaptor.getAllValues();

            TransactionEntry debitEntry = entries.get(0);
            TransactionEntry creditEntry = entries.get(1);

            assertEquals(amount, debitEntry.getAmount());
            assertEquals(
                    EntryDirection.DEBIT,
                    debitEntry.getEntryDirection()
            );
            assertSame(vaultAccount, debitEntry.getAccount());
            assertSame(savedHeader, debitEntry.getTransactionHeader());
            assertEquals(
                    "Vault withdrawal",
                    debitEntry.getDescription()
            );

            assertEquals(amount, creditEntry.getAmount());
            assertEquals(
                    EntryDirection.CREDIT,
                    creditEntry.getEntryDirection()
            );
            assertSame(userAccount, creditEntry.getAccount());
            assertSame(savedHeader, creditEntry.getTransactionHeader());
            assertEquals(
                    "Cash deposit",
                    creditEntry.getDescription()
            );

            assertEquals(
                    debitEntry.getCreatedAt(),
                    creditEntry.getCreatedAt()
            );
        }
    }


    @Nested
    class GetTransactionHistoryTests {

        @Test
        void shouldReturnTransactionHistoryWithDefaultFilters() {

            // Arrange
            TransactionHeader header =
                    TransactionHeader.builder()
                            .transactionType(TransactionType.DEPOSIT)
                            .createdAt(Instant.now())
                            .build();

            TransactionEntry entry =
                    TransactionEntry.builder()
                            .amount(new BigDecimal("500.00"))
                            .entryDirection(EntryDirection.CREDIT)
                            .account(userAccount)
                            .transactionHeader(header)
                            .description("Cash deposit")
                            .createdAt(header.getCreatedAt())
                            .build();

            Page<TransactionEntry> entries =
                    new PageImpl<>(List.of(entry));

            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            when(transactionEntryRepository.findAll(
                    any(Specification.class),
                    any(Pageable.class)))
                    .thenReturn(entries);

            // Act
            Page<TransactionHistoryDto> result =
                    transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "createdAt",
                            "DESC"
                    );

            // Assert
            assertEquals(1, result.getTotalElements());

            TransactionHistoryDto dto = result.getContent().get(0);

            assertEquals(
                    TransactionType.DEPOSIT,
                    dto.transactionType()
            );
            assertEquals(
                    EntryDirection.CREDIT,
                    dto.entryDirection()
            );
            assertEquals(
                    new BigDecimal("500.00"),
                    dto.amount()
            );
            assertEquals(
                    "Cash deposit",
                    dto.description()
            );

            verify(accountAuthorizationService)
                    .getOwnedAccount(accountNumber);

            verify(transactionEntryRepository)
                    .findAll(
                            any(Specification.class),
                            pageableCaptor.capture()
                    );

            Pageable pageable = pageableCaptor.getValue();

            assertEquals(0, pageable.getPageNumber());
            assertEquals(10, pageable.getPageSize());
            assertEquals(
                    Sort.Direction.DESC,
                    pageable.getSort().getOrderFor("createdAt").getDirection()
            );
        }


        @Test
        void shouldThrowExceptionWhenAccountDoesNotExist() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenThrow(new AccountNotFoundException(
                            "Account not found: " + accountNumber));

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenSortFieldIsInvalid() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "invalidField",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Invalid sort field. Allowed fields: [createdAt, amount]",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenSortDirectionIsInvalid() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "createdAt",
                            "INVALID"
                    )
            );

            // Assert
            assertEquals(
                    "sortDir must be either ASC or DESC.",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenPageIsNegative() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            InvalidPageRangeException exception = assertThrows(
                    InvalidPageRangeException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            -1,
                            10,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Page cannot be negative",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenSizeIsZero() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            InvalidSizeRangeException exception = assertThrows(
                    InvalidSizeRangeException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            0,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Size can be only from 1 to 100",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenSizeExceedsMaximum() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            InvalidSizeRangeException exception = assertThrows(
                    InvalidSizeRangeException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0,
                            101,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Size can be only from 1 to 100",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenMinimumAmountIsGreaterThanMaximumAmount() {

            // Arrange
            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            InvalidAmountRangeException exception = assertThrows(
                    InvalidAmountRangeException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            null,
                            null,
                            new BigDecimal("1000.00"),
                            new BigDecimal("500.00"),
                            null,
                            0,
                            10,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Minimum amount cannot be greater than maximum amount.",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }


        @Test
        void shouldThrowExceptionWhenStartDateIsAfterEndDate() {

            // Arrange
            Instant endDate = Instant.now();

            Instant startDate = endDate.plusSeconds(86400);

            when(accountAuthorizationService.getOwnedAccount(accountNumber))
                    .thenReturn(userAccount);

            // Act
            InvalidDateRangeException exception = assertThrows(
                    InvalidDateRangeException.class,
                    () -> transactionService.getTransactionHistory(
                            accountNumber,
                            startDate,
                            endDate,
                            null,
                            null,
                            null,
                            0,
                            10,
                            "createdAt",
                            "DESC"
                    )
            );

            // Assert
            assertEquals(
                    "Start date cannot be after end date.",
                    exception.getMessage()
            );

            verify(transactionEntryRepository, never())
                    .findAll(
                            any(Specification.class),
                            any(Pageable.class)
                    );
        }
    }
}