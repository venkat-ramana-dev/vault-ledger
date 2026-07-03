package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.exception.AccountNotFoundException;
import dev.venkat.vault_ledger.mapper.AccountMapper;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import dev.venkat.vault_ledger.service.impl.AccountServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements AccountServiceImpl {

    private final AccountRepository accountRepository;

    private final TransactionHeaderRepository transactionHeaderRepository;

    private final TransactionEntryRepository transactionEntryRepository;

    private final TransactionService transactionService;

    @Transactional
    @Override
    public AccountDto createAccount(User user, CreateAccountRequestDto createAccountRequestDto) {

        log.info("Creating new account for {}",
                createAccountRequestDto.accountHolderName());

        String accountHolderName = createAccountRequestDto.accountHolderName();
        String accountNumber = generateAccountNumber();
        log.info("Account number generated: {}", accountNumber);
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .accountStatus(AccountStatus.ACTIVE)
                .user(user)
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

            TransactionHeader transactionHeader = TransactionHeader.builder()
                    .transactionType(TransactionType.INITIAL_DEPOSIT)
                    .build();

            TransactionHeader savedTransactionHeader = transactionHeaderRepository.save(transactionHeader);

            TransactionEntry userTransactionEntry = TransactionEntry.builder()
                    .amount(createAccountRequestDto.initialDeposit())
                    .entryDirection(EntryDirection.CREDIT)
                    .account(savedAccount)
                    .transactionHeader(savedTransactionHeader)
                    .build();
            transactionEntryRepository.save(userTransactionEntry);

            TransactionEntry vaultTransactionEntry = TransactionEntry.builder()
                    .amount(createAccountRequestDto.initialDeposit())
                    .entryDirection(EntryDirection.DEBIT)
                    .account(vault) // USING THE VAULT OBJECT
                    .transactionHeader(savedTransactionHeader)
                    .build();
            transactionEntryRepository.save(vaultTransactionEntry);
        }
        BigDecimal startingBalance = createAccountRequestDto.initialDeposit();
        log.info("Initial deposit is processed.");
        return AccountMapper.mapToAccountDto(savedAccount, startingBalance);
    }

    @Transactional
    @Override
    public AccountDto getAccountDetails(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        BigDecimal currentBalance = transactionService.getAccountBalance(account.getId());
        return AccountMapper.mapToAccountDto(account, currentBalance);
    }

    @Transactional
    @Override
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        List<AccountDto> accountDtos = new ArrayList<>();
        for (Account account : accounts) {
            BigDecimal balance = transactionService.getAccountBalance(account.getId());
            AccountDto dto = AccountMapper.mapToAccountDto(account, balance);
            accountDtos.add(dto);
        }
        return accountDtos;
    }

    @Override
    public String deleteAccount(String accountNumber) {
        log.info("Closing account. {}",accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        account.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account closed successfully. {}",accountNumber);
        return "Account deleted successfully with Acc No : " + account.getAccountNumber();
    }

    private String generateAccountNumber() {

        long timestamp = System.currentTimeMillis();

        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        return "ACC-" + timestamp + "-" + randomSuffix;
    }
}
