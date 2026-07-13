package dev.venkat.vault_ledger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidPageRangeException extends RuntimeException {
    public InvalidPageRangeException(String message) {
        super(message);
    }
}
