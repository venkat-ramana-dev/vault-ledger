package dev.venkat.vault_ledger.exception;

public class AccountClosedException extends RuntimeException{

    public AccountClosedException(String message) {
        super(message);
    }
}
