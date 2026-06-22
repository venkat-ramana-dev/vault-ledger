package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {

    @Query("SELECT COALESCE(SUM(CASE WHEN t.entryDirection = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) " +
            "FROM TransactionEntry t WHERE t.account.id = :accountId")
    BigDecimal calculateBalanceByAccountId(@Param("accountId") Long accountId);
}
