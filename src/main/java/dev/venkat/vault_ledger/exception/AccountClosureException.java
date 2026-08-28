package dev.venkat.vault_ledger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class AccountClosureException extends RuntimeException{

    public AccountClosureException(String message) {
        super(message);
    }
}
