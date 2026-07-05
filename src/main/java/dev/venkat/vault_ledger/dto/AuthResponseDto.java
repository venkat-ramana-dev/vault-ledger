package dev.venkat.vault_ledger.dto;

import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.UserRole;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AuthResponseDto(String token,
                              String username,
                              UserRole role,
                              String accountNumber,
                              String accountHolderName,
                              AccountStatus accountStatus,
                              BigDecimal balance) { }
