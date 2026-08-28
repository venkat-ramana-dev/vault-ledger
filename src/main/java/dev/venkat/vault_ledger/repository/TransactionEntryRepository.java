package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionEntryRepository extends
        JpaRepository<TransactionEntry, Long>,
        JpaSpecificationExecutor<TransactionEntry> {

    @Query("SELECT COALESCE(SUM(CASE WHEN t.entryDirection = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) " +
            "FROM TransactionEntry t WHERE t.account.id = :accountId")
    BigDecimal calculateBalanceByAccountId(@Param("accountId") Long accountId);

    @EntityGraph(attributePaths = "transactionHeader")
    Page<TransactionEntry> findAll(
            Specification<TransactionEntry> spec,
            Pageable pageable
    );

    @Query("""
    SELECT t.account.id AS accountId,
           COALESCE(
               SUM(
                   CASE
                       WHEN t.entryDirection = 'CREDIT'
                       THEN t.amount
                       ELSE -t.amount
                   END
               ),
               0
           ) AS balance
    FROM TransactionEntry t
    WHERE t.account.id IN :accountIds
    GROUP BY t.account.id
    """)
    List<AccountBalanceProjection> calculateBalancesForAccounts(@Param("accountIds") List<Long> accountIds);
}
