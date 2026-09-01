package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// Prevent Spring from replacing PostgreSQL with an in-memory database.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Integration Tests: AccountRepository")
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = User.builder()
                .username("testuser")
                .password("password")
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        savedUser = userRepository.save(savedUser);
    }

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
    }

    @Nested
    @DisplayName("Method: findByAccountNumberForUpdate")
    class FindByAccountNumberForUpdateTests {

        @Test
        @DisplayName("Should return account when account number exists")
        void findByAccountNumberForUpdate_WhenAccountExists_ReturnsAccount() {

            // Arrange
            Account account = createAccount("ACC1234567890");
            Account savedAccount = accountRepository.save(account);

            // Act
            Optional<Account> result =
                    accountRepository.findByAccountNumberForUpdate(
                            savedAccount.getAccountNumber()
                    );

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getAccountNumber())
                    .isEqualTo(savedAccount.getAccountNumber());
        }

        @Test
        @DisplayName("Should return empty when account number does not exist")
        void findByAccountNumberForUpdate_WhenAccountDoesNotExist_ReturnsEmpty() {

            // Arrange
            String accountNumber = "ACC9999999999";

            // Act
            Optional<Account> result =
                    accountRepository.findByAccountNumberForUpdate(accountNumber);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Thread B reads updated state only after Thread A commits")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void findByAccountNumberForUpdate_WithPessimisticWrite_BlocksConcurrentReads()
                throws Exception {

            // Arrange
            Account account = createAccount("ACC1234567890");
            Account savedAccount = accountRepository.save(account);

            Long accountId = savedAccount.getId();
            String accountNumber = savedAccount.getAccountNumber();

            CountDownLatch threadAHasAcquiredLock =
                    new CountDownLatch(1);

            // Thread A
            CompletableFuture<Void> threadA =
                    CompletableFuture.runAsync(() -> {

                        transactionTemplate.execute(status -> {

                            Account lockedAccount =
                                    accountRepository
                                            .findByAccountNumberForUpdate(accountNumber)
                                            .orElseThrow();

                            // Change the account while holding the lock.
                            lockedAccount.setAccountStatus(
                                    AccountStatus.CLOSED
                            );

                            // Force the UPDATE to PostgreSQL while
                            // the transaction is still open.
                            entityManager.flush();

                            // Tell Thread B that the lock is acquired.
                            threadAHasAcquiredLock.countDown();

                            try {
                                // Keep the transaction open so Thread B
                                // has to wait for the lock.
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            return null;
                        });
                    });

            // Thread B
            CompletableFuture<AccountStatus> threadB =
                    CompletableFuture.supplyAsync(() -> {

                        try {
                            boolean lockAcquired =
                                    threadAHasAcquiredLock.await(
                                            5,
                                            TimeUnit.SECONDS
                                    );

                            if (!lockAcquired) {
                                throw new IllegalStateException(
                                        "Test timed out waiting for Thread A"
                                );
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }

                        return transactionTemplate.execute(status -> {

                            // This query waits until Thread A commits
                            // and releases the pessimistic lock.
                            Account lockedAccount =
                                    accountRepository
                                            .findByAccountNumberForUpdate(
                                                    accountNumber
                                            )
                                            .orElseThrow();

                            return lockedAccount.getAccountStatus();
                        });
                    });

            // Act
            CompletableFuture.allOf(threadA, threadB).join();

            // Assert
            // Thread B sees CLOSED only after Thread A's transaction
            // commits and releases the database lock.
            assertThat(threadB.join())
                    .isEqualTo(AccountStatus.CLOSED);

            Account finalAccount =
                    accountRepository
                            .findByAccountNumber(accountNumber)
                            .orElseThrow();

            assertThat(finalAccount.getAccountStatus())
                    .isEqualTo(AccountStatus.CLOSED);
        }
    }

    private Account createAccount(String accountNumber) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName("Test User")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(savedUser)
                .build();
    }
}