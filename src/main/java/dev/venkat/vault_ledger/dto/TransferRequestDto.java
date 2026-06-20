package dev.venkat.vault_ledger.dto;

import java.math.BigDecimal;

public record TransferRequestDto(String toAccount,
                                 String fromAccount,
                                 BigDecimal amount) {
}
