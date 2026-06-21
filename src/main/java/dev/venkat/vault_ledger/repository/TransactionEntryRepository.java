package dev.venkat.vault_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEntryRepository extends JpaRepository<TransactionRepository, Long> {
}
