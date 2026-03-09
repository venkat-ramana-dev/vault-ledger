package dev.venkat.vault_ledger.dto;

public record TransferDto(Long toId,
                          AmountDto amountDto) {
}
