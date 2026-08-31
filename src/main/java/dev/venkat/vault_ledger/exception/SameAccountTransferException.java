package dev.venkat.vault_ledger.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException(String message) {
        super(message);
    }
}
