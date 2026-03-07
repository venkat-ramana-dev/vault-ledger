package dev.venkat.vault_ledger.dto;

import lombok.Builder;

@Builder
public record AccountDto(Long id,
                         String accountHolderName) { }
