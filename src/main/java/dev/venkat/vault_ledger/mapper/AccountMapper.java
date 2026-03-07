package dev.venkat.vault_ledger.mapper;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.entity.Account;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountHolderName(account.getAccountHolderName())
                .build();
    }

    public static Account mapToAccount (AccountDto accountDto) {
        return Account.builder()
                .id(accountDto.id())
                .accountHolderName(accountDto.accountHolderName())
                .build();
    }

}