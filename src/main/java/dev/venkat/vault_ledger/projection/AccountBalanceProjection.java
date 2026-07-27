package dev.venkat.vault_ledger.projection;

import java.math.BigDecimal;

public interface AccountBalanceProjection {
    Long getAccountId();
    BigDecimal getBalance();
}
