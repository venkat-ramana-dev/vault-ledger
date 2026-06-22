package dev.venkat.vault_ledger.mapper;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;

import java.math.BigDecimal;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account account, BigDecimal balance) {
        return AccountDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .accountStatus(account.getAccountStatus())
                .balance(balance)
                .build();
    }

    public static Account mapToAccount (AccountDto accountDto) {
        return Account.builder()
                .accountNumber(accountDto.accountNumber())
                .accountHolderName(accountDto.accountHolderName())
                .accountStatus(accountDto.accountStatus())
                .build();
    }

}