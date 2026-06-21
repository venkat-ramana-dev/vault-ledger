package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {
}
