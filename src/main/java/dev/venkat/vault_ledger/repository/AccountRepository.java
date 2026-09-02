package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    //"SELECT ... FOR UPDATE"
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    Optional<Account> findByAccountNumberAndUser_Username(
            String accountNumber,
            String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM Account a
        WHERE a.accountNumber = :accountNumber
        AND a.user.username = :username
        """)
    Optional<Account> findOwnedAccountForUpdate(
            @Param("accountNumber") String accountNumber,
            @Param("username") String username
    );

    Optional<Account> findByUser(User user);
}
