package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.enums.AccountStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountDto(Long id,
                         String accountHolderName,
                         BigDecimal balance,
                         AccountStatus accountStatus) { }
