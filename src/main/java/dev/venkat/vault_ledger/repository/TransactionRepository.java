package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
