package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {

    @Query("SELECT COALESCE(SUM(CASE WHEN t.entryDirection = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) " +
            "FROM TransactionEntry t WHERE t.account.id = :accountId")
    BigDecimal calculateBalanceByAccountId(@Param("accountId") Long accountId);

//    SELECT te.* FROM transaction_entries te
//    JOIN accounts a ON te.account_id = a.id
//    JOIN transaction_headers th ON te.header_id = th.id
//    WHERE a.account_number = ?
//    ORDER BY th.created_at DESC;
    List<TransactionEntry> findByAccount_AccountNumberOrderByTransactionHeader_CreatedAtDesc(String accountNumber);
}
