package dev.venkat.vault_ledger.bootstrap;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VaultInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    // Define the constant so you can use it safely anywhere in your app
    public static final String SYSTEM_VAULT_ACCOUNT_NUMBER = "SYS-VAULT-0000";

    public VaultInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Check if the Vault already exists
        Optional<Account> vault = accountRepository.findByAccountNumber(SYSTEM_VAULT_ACCOUNT_NUMBER);

        // 2. If it does not exist, create it
        if (vault.isEmpty()) {
            Account systemVault = Account.builder()
                    .accountNumber(SYSTEM_VAULT_ACCOUNT_NUMBER)
                    .accountHolderName("Vault Ledger System Account")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            accountRepository.save(systemVault);
            System.out.println("Vault Account created successfully.");
        } else {
            System.out.println("Vault Account already exist.");
        }
    }
}
