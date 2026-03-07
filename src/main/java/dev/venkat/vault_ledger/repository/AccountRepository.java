package dev.venkat.vault_ledger.repository;

import dev.venkat.vault_ledger.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
