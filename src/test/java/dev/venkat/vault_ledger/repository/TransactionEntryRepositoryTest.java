package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// Use the real PostgreSQL Testcontainer instead of an in-memory database.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Integration Tests: TransactionEntryRepository")
class TransactionEntryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TransactionEntryRepository transactionEntryRepository;

    @Autowired
    private TransactionHeaderRepository transactionHeaderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User savedUser;
    private Account account;
    private Account secondAccount;

    @BeforeEach
    void setUp() {

        // Clean test database before every test.
        // This is the temporary Testcontainers database, not the
        // PostgreSQL database used by Docker Compose.
        transactionEntryRepository.deleteAll();
        transactionHeaderRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(
                User.builder()
                        .username("testuser")
                        .password("password")
                        .userRole(UserRole.USER)
                        .createdAt(Instant.now())
                        .build()
        );

        account = accountRepository.save(
                createAccount("ACC1000000001", savedUser)
        );

        secondAccount = accountRepository.save(
                createAccount("ACC1000000002", savedUser)
        );
    }

    @AfterEach
    void tearDown() {

        // Keep the test database clean after every test.
        transactionEntryRepository.deleteAll();
        transactionHeaderRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Method: calculateBalanceByAccountId")
    class CalculateBalanceByAccountIdTests {

        @Test
        @DisplayName("Should return zero when account has no transaction entries")
        void calculateBalanceByAccountId_WhenNoEntries_ReturnsZero() {

            // Arrange
            Long accountId = account.getId();

            // Act
            BigDecimal balance =
                    transactionEntryRepository.calculateBalanceByAccountId(
                            accountId
                    );

            // Assert
            // Important because the query explicitly uses COALESCE(..., 0).
            assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return total credit amount")
        void calculateBalanceByAccountId_WhenOnlyCredits_ReturnsCreditTotal() {

            // Arrange
            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("1000")
            );

            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("500")
            );

            // Act
            BigDecimal balance =
                    transactionEntryRepository.calculateBalanceByAccountId(
                            account.getId()
                    );

            // Assert
            assertThat(balance).isEqualByComparingTo(
                    new BigDecimal("1500")
            );
        }

        @Test
        @DisplayName("Should subtract debit amounts from credit amounts")
        void calculateBalanceByAccountId_WhenCreditsAndDebits_ReturnsCorrectBalance() {

            // Arrange
            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("1000")
            );

            createTransactionEntry(
                    account,
                    TransactionType.WITHDRAWAL,
                    EntryDirection.DEBIT,
                    new BigDecimal("300")
            );

            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("500")
            );

            // Act
            BigDecimal balance =
                    transactionEntryRepository.calculateBalanceByAccountId(
                            account.getId()
                    );

            // Assert
            // Expected: 1000 + 500 - 300 = 1200
            assertThat(balance).isEqualByComparingTo(
                    new BigDecimal("1200")
            );
        }

        @Test
        @DisplayName("Should calculate balance only for requested account")
        void calculateBalanceByAccountId_WhenOtherAccountHasEntries_IgnoresOtherAccount() {

            // Arrange
            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("1000")
            );

            createTransactionEntry(
                    secondAccount,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("5000")
            );

            // Act
            BigDecimal balance =
                    transactionEntryRepository.calculateBalanceByAccountId(
                            account.getId()
                    );

            // Assert
            assertThat(balance).isEqualByComparingTo(
                    new BigDecimal("1000")
            );
        }
    }

    @Nested
    @DisplayName("Method: calculateBalancesForAccounts")
    class CalculateBalancesForAccountsTests {

        @Test
        @DisplayName("Should return balance for multiple accounts")
        void calculateBalancesForAccounts_WhenMultipleAccounts_ReturnsBalances() {

            // Arrange
            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("1000")
            );

            createTransactionEntry(
                    account,
                    TransactionType.WITHDRAWAL,
                    EntryDirection.DEBIT,
                    new BigDecimal("200")
            );

            createTransactionEntry(
                    secondAccount,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("3000")
            );

            List<Long> accountIds =
                    List.of(account.getId(), secondAccount.getId());

            // Act
            List<AccountBalanceProjection> balances =
                    transactionEntryRepository.calculateBalancesForAccounts(
                            accountIds
                    );

            // Assert
            assertThat(balances).hasSize(2);

            AccountBalanceProjection firstBalance =
                    balances.stream()
                            .filter(b -> b.getAccountId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow();

            AccountBalanceProjection secondBalance =
                    balances.stream()
                            .filter(b -> b.getAccountId().equals(secondAccount.getId()))
                            .findFirst()
                            .orElseThrow();

            assertThat(firstBalance.getBalance())
                    .isEqualByComparingTo(new BigDecimal("800"));

            assertThat(secondBalance.getBalance())
                    .isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        @DisplayName("Should not return accounts that have no transaction entries")
        void calculateBalancesForAccounts_WhenAccountHasNoEntries_DoesNotReturnAccount() {

            // Arrange
            // Only the first account receives a transaction.
            createTransactionEntry(
                    account,
                    TransactionType.DEPOSIT,
                    EntryDirection.CREDIT,
                    new BigDecimal("1000")
            );

            List<Long> accountIds =
                    List.of(account.getId(), secondAccount.getId());

            // Act
            List<AccountBalanceProjection> balances =
                    transactionEntryRepository.calculateBalancesForAccounts(
                            accountIds
                    );

            // Assert
            // The query uses GROUP BY on TransactionEntry, so an account
            // with no entries has no group and is not returned.
            assertThat(balances)
                    .extracting(AccountBalanceProjection::getAccountId)
                    .containsExactly(account.getId());
        }
    }

    private Account createAccount(
            String accountNumber,
            User user
    ) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName("Test User")
                .accountStatus(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private TransactionEntry createTransactionEntry(
            Account account,
            TransactionType transactionType,
            EntryDirection direction,
            BigDecimal amount
    ) {
        Instant createdAt = Instant.now();

        TransactionHeader header =
                TransactionHeader.builder()
                        .transactionType(transactionType)
                        .createdAt(createdAt)
                        .build();

        header = transactionHeaderRepository.save(header);

        TransactionEntry entry =
                TransactionEntry.builder()
                        .amount(amount)
                        .entryDirection(direction)
                        .account(account)
                        .transactionHeader(header)
                        .description("Test transaction")
                        .createdAt(createdAt)
                        .build();

        return transactionEntryRepository.save(entry);
    }
}
