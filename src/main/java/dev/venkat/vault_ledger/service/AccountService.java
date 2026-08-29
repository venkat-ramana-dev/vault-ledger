package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.exception.AccountClosedException;
import dev.venkat.vault_ledger.exception.AccountClosureException;
import dev.venkat.vault_ledger.exception.AccountNotFoundException;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.projection.AccountBalanceProjection;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.util.TransactionDescriptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private final TransactionService transactionService;

    @Transactional
    public AccountDto createAccount(User user, CreateAccountRequestDto createAccountRequestDto) {

        log.info("Creating new account for {}",
                createAccountRequestDto.accountHolderName());

        String accountHolderName = createAccountRequestDto.accountHolderName();
        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .accountStatus(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(user.getCreatedAt())
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("New account is created successfully. Account Number: {}. Account Holder Name: {}",
                accountNumber,
                accountHolderName);

        if (createAccountRequestDto.initialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Processing initial deposit of {} for account {}",
                    createAccountRequestDto.initialDeposit(),
                    savedAccount);
            Account vault = accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                    .orElseThrow(() -> new AccountNotFoundException("System error: Vault account not found. AccountNumber: " +
                            VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER));

            transactionService.createDoubleEntryTransaction(
                    TransactionType.INITIAL_DEPOSIT,
                    createAccountRequestDto.initialDeposit(),
                    vault,
                    TransactionDescriptionUtil.systemVaultWithdrawal(savedAccount),
                    savedAccount,
                    TransactionDescriptionUtil.initialDeposit()
            );
        }
        BigDecimal startingBalance = createAccountRequestDto.initialDeposit();
        log.info("Initial deposit is processed.");
        return AccountMapper.mapToAccountDto(savedAccount, startingBalance);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountDetails(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        BigDecimal currentBalance = transactionService.getAccountBalance(account.getId());
        return AccountMapper.mapToAccountDto(account, currentBalance);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountDetails(User user) {
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for username: " + user.getUsername()));
        BigDecimal currentBalance = transactionService.getAccountBalance(account.getId());
        return AccountMapper.mapToAccountDto(account, currentBalance);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {

        List<Account> accounts = accountRepository.findAll();

        if (accounts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> accountIds = new ArrayList<>();
        for (Account account : accounts) {
            accountIds.add(account.getId());
        }

        List<AccountBalanceProjection> balances = transactionService.getAccountBalances(accountIds);

        Map<Long, BigDecimal> balanceMap = new HashMap<>();
        for (AccountBalanceProjection b : balances) {
            balanceMap.put(b.getAccountId(), b.getBalance());
        }

        List<AccountDto> result = new ArrayList<>();
        for (Account account : accounts) {
            BigDecimal balance = balanceMap.getOrDefault(account.getId(), BigDecimal.ZERO);
            AccountDto dto = AccountMapper.mapToAccountDto(account, balance);
            result.add(dto);
        }

        return result;
    }

    @Transactional
    public String closeAccount(String accountNumber) {
        log.info("Closing account. {}",accountNumber);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account is already closed: " + accountNumber);
        }

        BigDecimal balance = transactionService.getAccountBalance(account.getId());

        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountClosureException("Account cannot be closed because its balance is not zero. Current balance: " + balance);
        }

        account.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account closed successfully. {}",accountNumber);
        return "Account closed successfully with Acc No : " + account.getAccountNumber();
    }


    private String generateAccountNumber() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            long randomNum = java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
            String accountNumber = "ACC" + randomNum;

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            } else {
                log.warn("Account number collision detected for {}. Retrying... (Attempt {}/{})",
                        accountNumber, i + 1, maxRetries);
            }
        }

        log.error("CRITICAL: Failed to generate a unique account number after {} attempts. Is the database full?", maxRetries);
        throw new IllegalStateException("System busy: Could not generate a unique account number.");
    }
}
