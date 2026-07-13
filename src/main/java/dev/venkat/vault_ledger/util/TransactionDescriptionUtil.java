package dev.venkat.vault_ledger.util;

import dev.venkat.vault_ledger.entity.Account;

public final class TransactionDescriptionUtil {

    private TransactionDescriptionUtil() {
    }

    public static String transferTo(Account receiver) {
        return "Transfer to " + receiver.getAccountHolderName();
    }

    public static String transferFrom(Account sender) {
        return "Transfer from " + sender.getAccountHolderName();
    }

    public static String cashDeposit() {
        return "Cash Deposit";
    }

    public static String cashWithdrawal() {
        return "Cash Withdrawal";
    }

    public static String initialDeposit() {
        return "Initial Deposit";
    }

    public static String systemVaultDeposit(Account account) {
        return "System Vault Deposit - " + account.getAccountHolderName();
    }

    public static String systemVaultWithdrawal(Account account) {
        return "System Vault Withdrawal - " + account.getAccountHolderName();
    }
}
