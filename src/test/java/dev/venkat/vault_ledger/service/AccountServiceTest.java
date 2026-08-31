package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.exception.AccountClosedException;
import dev.venkat.vault_ledger.exception.AccountClosureException;
import dev.venkat.vault_ledger.exception.AccountNotFoundException;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import dev.venkat.vault_ledger.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Captor
    private ArgumentCaptor<List<Long>> accountIdsCaptor;

    private User user;
    private String accountNumber;
    private String accountHolderName;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("venkat")
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        accountNumber = "ACC1234567890";
        accountHolderName = "Venkat Ramana";
    }

    private Account createAccount() {
        return Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .accountStatus(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Nested
    class CreateAccountTests {

        @Test
        void shouldCreateAccountAndProcessInitialDeposit() {

            // Arrange
            CreateAccountRequestDto request = CreateAccountRequestDto.builder()
                    .accountHolderName(accountHolderName)
                    .initialDeposit(new BigDecimal("1000.00"))
                    .build();

            Account savedAccount = createAccount();

            Account vaultAccount = Account.builder()
                    .id(2L)
                    .accountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(accountRepository.existsByAccountNumber(anyString()))
                    .thenReturn(false);

            when(accountRepository.save(any(Account.class)))
                    .thenReturn(savedAccount);

            when(accountRepository.findByAccountNumber(
                    VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(vaultAccount));

            // Act
            AccountDto result = accountService.createAccount(user, request);

            // Assert
            verify(accountRepository).save(accountCaptor.capture());

            Account savedAccountArgument = accountCaptor.getValue();

            assertEquals(
                    accountHolderName,
                    savedAccountArgument.getAccountHolderName()
            );
            assertEquals(
                    AccountStatus.ACTIVE,
                    savedAccountArgument.getAccountStatus()
            );
            assertSame(user, savedAccountArgument.getUser());
            assertEquals(
                    user.getCreatedAt(),
                    savedAccountArgument.getCreatedAt()
            );

            assertNotNull(savedAccountArgument.getAccountNumber());
            assertTrue(
                    savedAccountArgument.getAccountNumber()
                            .matches("ACC\\d{10}")
            );

            verify(accountRepository)
                    .findByAccountNumber(
                            VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER
                    );

            verify(transactionService).createDoubleEntryTransaction(
                    eq(TransactionType.INITIAL_DEPOSIT),
                    eq(request.initialDeposit()),
                    eq(vaultAccount),
                    anyString(),
                    eq(savedAccount),
                    anyString()
            );

            assertEquals(
                    savedAccount.getAccountNumber(),
                    result.accountNumber()
            );
            assertEquals(
                    savedAccount.getAccountHolderName(),
                    result.accountHolderName()
            );
            assertEquals(
                    savedAccount.getAccountStatus(),
                    result.accountStatus()
            );
            assertEquals(
                    request.initialDeposit(),
                    result.balance()
            );

            verify(accountRepository)
                    .existsByAccountNumber(anyString());
        }

        @Test
        void shouldCreateAccountWithoutProcessingInitialDepositWhenAmountIsZero() {

            // Arrange
            CreateAccountRequestDto request = CreateAccountRequestDto.builder()
                    .accountHolderName(accountHolderName)
                    .initialDeposit(BigDecimal.ZERO)
                    .build();

            Account savedAccount = createAccount();

            when(accountRepository.existsByAccountNumber(anyString()))
                    .thenReturn(false);

            when(accountRepository.save(any(Account.class)))
                    .thenReturn(savedAccount);

            // Act
            AccountDto result = accountService.createAccount(user, request);

            // Assert
            verify(accountRepository).save(accountCaptor.capture());

            Account savedAccountArgument = accountCaptor.getValue();

            assertEquals(
                    accountHolderName,
                    savedAccountArgument.getAccountHolderName()
            );
            assertEquals(
                    AccountStatus.ACTIVE,
                    savedAccountArgument.getAccountStatus()
            );
            assertSame(user, savedAccountArgument.getUser());

            assertEquals(BigDecimal.ZERO, result.balance());

            verify(accountRepository, never())
                    .findByAccountNumber(
                            VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER
                    );

            verify(transactionService, never())
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );
        }

        @Test
        void shouldRetryAccountNumberGenerationAfterCollision() {

            // Arrange
            CreateAccountRequestDto request = CreateAccountRequestDto.builder()
                    .accountHolderName(accountHolderName)
                    .initialDeposit(BigDecimal.ZERO)
                    .build();

            Account savedAccount = createAccount();

            when(accountRepository.existsByAccountNumber(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);

            when(accountRepository.save(any(Account.class)))
                    .thenReturn(savedAccount);

            // Act
            AccountDto result = accountService.createAccount(user, request);

            // Assert
            verify(accountRepository, times(2))
                    .existsByAccountNumber(anyString());

            verify(accountRepository).save(any(Account.class));

            assertNotNull(result.accountNumber());
            assertTrue(
                    result.accountNumber().matches("ACC\\d{10}")
            );
        }

        @Test
        void shouldThrowExceptionWhenAccountNumberGenerationExceedsRetries() {

            // Arrange
            CreateAccountRequestDto request = CreateAccountRequestDto.builder()
                    .accountHolderName(accountHolderName)
                    .initialDeposit(BigDecimal.ZERO)
                    .build();

            when(accountRepository.existsByAccountNumber(anyString()))
                    .thenReturn(true);

            // Act
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> accountService.createAccount(user, request)
            );

            // Assert
            assertEquals(
                    "System busy: Could not generate a unique account number.",
                    exception.getMessage()
            );

            verify(accountRepository, times(3))
                    .existsByAccountNumber(anyString());

            verify(accountRepository, never())
                    .save(any(Account.class));

            verify(transactionService, never())
                    .createDoubleEntryTransaction(
                            any(TransactionType.class),
                            any(BigDecimal.class),
                            any(Account.class),
                            anyString(),
                            any(Account.class),
                            anyString()
                    );
        }
    }

    @Nested
    class GetAccountDetailsByAccountNumberTests {

        @Test
        void shouldReturnAccountDetailsWhenAccountExists() {

            // Arrange
            Account account = createAccount();
            BigDecimal balance = new BigDecimal("1500.00");

            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.of(account));

            when(transactionService.getAccountBalance(account.getId()))
                    .thenReturn(balance);

            // Act
            AccountDto result =
                    accountService.getAccountDetails(accountNumber);

            // Assert
            assertEquals(
                    account.getAccountNumber(),
                    result.accountNumber()
            );
            assertEquals(
                    account.getAccountHolderName(),
                    result.accountHolderName()
            );
            assertEquals(
                    account.getAccountStatus(),
                    result.accountStatus()
            );
            assertEquals(balance, result.balance());

            verify(accountRepository)
                    .findByAccountNumber(accountNumber);

            verify(transactionService)
                    .getAccountBalance(account.getId());
        }

        @Test
        void shouldThrowExceptionWhenAccountDoesNotExist() {

            // Arrange
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.empty());

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> accountService.getAccountDetails(accountNumber)
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(accountRepository)
                    .findByAccountNumber(accountNumber);

            verify(transactionService, never())
                    .getAccountBalance(anyLong());
        }
    }

    @Nested
    class GetAccountDetailsByUserTests {

        @Test
        void shouldReturnAccountDetailsWhenAccountExists() {

            // Arrange
            Account account = createAccount();
            BigDecimal balance = new BigDecimal("2000.00");

            when(accountRepository.findByUser(user))
                    .thenReturn(Optional.of(account));

            when(transactionService.getAccountBalance(account.getId()))
                    .thenReturn(balance);

            // Act
            AccountDto result =
                    accountService.getAccountDetails(user);

            // Assert
            assertEquals(
                    account.getAccountNumber(),
                    result.accountNumber()
            );
            assertEquals(
                    account.getAccountHolderName(),
                    result.accountHolderName()
            );
            assertEquals(
                    account.getAccountStatus(),
                    result.accountStatus()
            );
            assertEquals(balance, result.balance());

            verify(accountRepository)
                    .findByUser(user);

            verify(transactionService)
                    .getAccountBalance(account.getId());
        }

        @Test
        void shouldThrowExceptionWhenAccountDoesNotExistForUser() {

            // Arrange
            when(accountRepository.findByUser(user))
                    .thenReturn(Optional.empty());

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> accountService.getAccountDetails(user)
            );

            // Assert
            assertEquals(
                    "Account not found for username: " + user.getUsername(),
                    exception.getMessage()
            );

            verify(accountRepository)
                    .findByUser(user);

            verify(transactionService, never())
                    .getAccountBalance(anyLong());
        }
    }

    @Nested
    class GetAllAccountsTests {

        @Test
        void shouldReturnAllAccountsWithBalances() {

            // Arrange
            Account firstAccount = createAccount();

            Account secondAccount = Account.builder()
                    .id(2L)
                    .accountNumber("ACC2222222222")
                    .accountHolderName("Ramana")
                    .accountStatus(AccountStatus.ACTIVE)
                    .user(user)
                    .createdAt(user.getCreatedAt())
                    .build();

            List<Account> accounts = List.of(
                    firstAccount,
                    secondAccount
            );

            AccountBalanceProjection firstBalance =
                    mock(AccountBalanceProjection.class);

            AccountBalanceProjection secondBalance =
                    mock(AccountBalanceProjection.class);

            when(firstBalance.getAccountId())
                    .thenReturn(1L);

            when(firstBalance.getBalance())
                    .thenReturn(new BigDecimal("1000.00"));

            when(secondBalance.getAccountId())
                    .thenReturn(2L);

            when(secondBalance.getBalance())
                    .thenReturn(new BigDecimal("2500.00"));

            when(accountRepository.findAll())
                    .thenReturn(accounts);

            when(transactionService.getAccountBalances(anyList()))
                    .thenReturn(List.of(
                            firstBalance,
                            secondBalance
                    ));

            // Act
            List<AccountDto> result =
                    accountService.getAllAccounts();

            // Assert
            assertEquals(2, result.size());

            assertEquals(
                    new BigDecimal("1000.00"),
                    result.get(0).balance()
            );

            assertEquals(
                    new BigDecimal("2500.00"),
                    result.get(1).balance()
            );

            assertEquals(
                    firstAccount.getAccountNumber(),
                    result.get(0).accountNumber()
            );

            assertEquals(
                    secondAccount.getAccountNumber(),
                    result.get(1).accountNumber()
            );

            verify(accountRepository).findAll();

            verify(transactionService)
                    .getAccountBalances(accountIdsCaptor.capture());

            assertEquals(
                    List.of(1L, 2L),
                    accountIdsCaptor.getValue()
            );
        }

        @Test
        void shouldReturnEmptyListWhenNoAccountsExist() {

            // Arrange
            when(accountRepository.findAll())
                    .thenReturn(List.of());

            // Act
            List<AccountDto> result =
                    accountService.getAllAccounts();

            // Assert
            assertTrue(result.isEmpty());

            verify(accountRepository).findAll();

            verify(transactionService, never())
                    .getAccountBalances(anyList());
        }

        @Test
        void shouldUseZeroBalanceWhenBalanceProjectionIsMissing() {

            // Arrange
            Account account = createAccount();

            when(accountRepository.findAll())
                    .thenReturn(List.of(account));

            when(transactionService.getAccountBalances(List.of(1L)))
                    .thenReturn(List.of());

            // Act
            List<AccountDto> result =
                    accountService.getAllAccounts();

            // Assert
            assertEquals(1, result.size());

            assertEquals(
                    BigDecimal.ZERO,
                    result.get(0).balance()
            );

            verify(accountRepository).findAll();

            verify(transactionService)
                    .getAccountBalances(List.of(1L));
        }
    }

    @Nested
    class CloseAccountTests {

        @Test
        void shouldThrowExceptionWhenAccountDoesNotExist() {

            // Arrange
            when(accountRepository.findByAccountNumberForUpdate(accountNumber))
                    .thenReturn(Optional.empty());

            // Act
            AccountNotFoundException exception = assertThrows(
                    AccountNotFoundException.class,
                    () -> accountService.closeAccount(accountNumber)
            );

            // Assert
            assertEquals(
                    "Account not found: " + accountNumber,
                    exception.getMessage()
            );

            verify(accountRepository)
                    .findByAccountNumberForUpdate(accountNumber);

            verify(transactionService, never())
                    .getAccountBalance(anyLong());

            verify(accountRepository, never())
                    .save(any(Account.class));
        }

        @Test
        void shouldThrowExceptionWhenAccountIsAlreadyClosed() {

            // Arrange
            Account account = createAccount();
            account.setAccountStatus(AccountStatus.CLOSED);

            when(accountRepository.findByAccountNumberForUpdate(accountNumber))
                    .thenReturn(Optional.of(account));

            // Act
            AccountClosedException exception = assertThrows(
                    AccountClosedException.class,
                    () -> accountService.closeAccount(accountNumber)
            );

            // Assert
            assertEquals(
                    "Account is already closed: " + accountNumber,
                    exception.getMessage()
            );

            verify(transactionService, never())
                    .getAccountBalance(anyLong());

            verify(accountRepository, never())
                    .save(any(Account.class));
        }

        @Test
        void shouldThrowExceptionWhenAccountBalanceIsNotZero() {

            // Arrange
            Account account = createAccount();
            BigDecimal balance = new BigDecimal("500.00");

            when(accountRepository.findByAccountNumberForUpdate(accountNumber))
                    .thenReturn(Optional.of(account));

            when(transactionService.getAccountBalance(account.getId()))
                    .thenReturn(balance);

            // Act
            AccountClosureException exception = assertThrows(
                    AccountClosureException.class,
                    () -> accountService.closeAccount(accountNumber)
            );

            // Assert
            assertEquals(
                    "Account cannot be closed because its balance is not zero. Current balance: "
                            + balance,
                    exception.getMessage()
            );

            verify(transactionService)
                    .getAccountBalance(account.getId());

            assertEquals(
                    AccountStatus.ACTIVE,
                    account.getAccountStatus()
            );

            verify(accountRepository, never())
                    .save(any(Account.class));
        }

        @Test
        void shouldCloseAccountWhenBalanceIsZero() {

            // Arrange
            Account account = createAccount();

            when(accountRepository.findByAccountNumberForUpdate(accountNumber))
                    .thenReturn(Optional.of(account));

            when(transactionService.getAccountBalance(account.getId()))
                    .thenReturn(BigDecimal.ZERO);

            // Act
            String result =
                    accountService.closeAccount(accountNumber);

            // Assert
            assertEquals(
                    AccountStatus.CLOSED,
                    account.getAccountStatus()
            );

            verify(transactionService)
                    .getAccountBalance(account.getId());

            verify(accountRepository).save(account);

            assertEquals(
                    "Account closed successfully with Acc No : "
                            + accountNumber,
                    result
            );
        }
    }
}