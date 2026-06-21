package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.TransactionHeader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHeaderRepository extends JpaRepository<TransactionHeader, Long> {
}
