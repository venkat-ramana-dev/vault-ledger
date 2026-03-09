package dev.venkat.vault_ledger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InSufficientBalanceException extends RuntimeException{

    public InSufficientBalanceException(String message) {
        super(message);
    }
}
