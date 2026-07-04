package dev.venkat.vault_ledger.dto;

import lombok.Builder;

@Builder
public record LoginRequestDto (String username,
                               String password){}
