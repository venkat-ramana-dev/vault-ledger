package dev.venkat.vault_ledger.dto;

import java.math.BigDecimal;

public record TransferDto(Long toId,
                          BigDecimal amount) {
}
