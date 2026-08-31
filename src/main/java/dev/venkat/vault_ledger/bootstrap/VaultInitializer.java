package dev.venkat.vault_ledger.bootstrap;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class VaultInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public static final String SYSTEM_VAULT_ACCOUNT_NUMBER = "SYS-VAULT-0000";

    @Value("${bootstrap.system-vault.username}")
    private String systemVaultUsername;

    @Value("${bootstrap.system-vault.password}")
    private String systemVaultPassword;

    @Value("${bootstrap.admin.username}")
    private String adminUsername;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    public VaultInitializer(AccountRepository accountRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        Optional<Account> vault = accountRepository.findByAccountNumber(SYSTEM_VAULT_ACCOUNT_NUMBER);

        if (vault.isEmpty()) {

            Instant createdAt = Instant.now();

            User system = User.builder()
                    .username(systemVaultUsername)
                    .userRole(UserRole.SYSTEM)
                    .password(passwordEncoder.encode(systemVaultPassword))
                    .createdAt(createdAt)
                    .build();

            userRepository.save(system);
            log.info("Admin vault-system created successfully.");

            Account systemVault = Account.builder()
                    .accountNumber(SYSTEM_VAULT_ACCOUNT_NUMBER)
                    .accountHolderName("Vault Ledger System Account")
                    .accountStatus(AccountStatus.ACTIVE)
                    .user(system)
                    .createdAt(createdAt)
                    .build();

            accountRepository.save(systemVault);
            log.info("Vault Account created successfully.");
        } else {
            log.info("Vault Account verified.");
        }

        Optional<User> user = userRepository.findByUsername(adminUsername);
        if (user.isEmpty()) {

            User admin = User.builder()
                    .username(adminUsername)
                    .userRole(UserRole.ADMIN)
                    .password(passwordEncoder.encode(adminPassword))
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Admin created successfully.");
        }
    }
}
