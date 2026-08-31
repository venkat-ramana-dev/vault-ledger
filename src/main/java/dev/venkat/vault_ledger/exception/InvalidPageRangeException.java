package dev.venkat.vault_ledger.exception;

public class InvalidPageRangeException extends RuntimeException {
    public InvalidPageRangeException(String message) {
        super(message);
    }
}
