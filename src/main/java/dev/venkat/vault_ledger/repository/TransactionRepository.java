package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // SELECT * FROM transactions WHERE account_id = ?
    List<Transaction> findByAccountId(Long accountId);
}
