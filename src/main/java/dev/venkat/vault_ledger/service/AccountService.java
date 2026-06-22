package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.bootstrap.VaultInitializer;
import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountServiceImpl {

    private final AccountRepository accountRepository;

    private final TransactionHeaderRepository transactionHeaderRepository;

    private final TransactionEntryRepository transactionEntryRepository;

    @Transactional
    @Override
    public AccountDto createAccount(CreateAccountRequestDto createAccountRequestDto) {

        if (createAccountRequestDto.initialDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }

        String accountHolderName = createAccountRequestDto.accountHolderName();
        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        if (createAccountRequestDto.initialDeposit().compareTo(BigDecimal.ZERO) > 0) {

            Account vault = accountRepository.findByAccountNumber(VaultInitializer.SYSTEM_VAULT_ACCOUNT_NUMBER)
                    .orElseThrow(() -> new IllegalStateException("System error: Vault account not found"));

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

        return AccountMapper.mapToAccountDto(savedAccount, startingBalance);
    }

//    @Override
//    public AccountDto getAccountById(Long id) {
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with id :" + id));
//
//        AccountDto accountDto = AccountMapper.mapToAccountDto(account);
//        return accountDto;
//    }
//
//    @Override
//    public List<AccountDto> getAllAccounts() {
//
//        List<AccountDto> accountDtos = new ArrayList<>();
//
//        accountDtos = accountRepository.findAll().stream()
//                .map(account -> AccountMapper.mapToAccountDto(account))
//                .toList();
//
//        return accountDtos;
//    }
//
//    @Override
//    public String deleteAccountById(Long id) {
//
//        Account account = getAccountEntityById(id);
//        account.setAccountStatus(AccountStatus.CLOSED);
//        accountRepository.save(account);
//        return "Account deleted successfully with id " + id;
//    }
//
//    @Override
//    public Account getAccountEntityById(Long id) {
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() ->new RuntimeException("Account not found with id :" + id));
//
//        return account;
//    }

    private String generateAccountNumber() {

        long timestamp = System.currentTimeMillis();

        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        return "ACC-" + timestamp + "-" + randomSuffix;
    }
}
