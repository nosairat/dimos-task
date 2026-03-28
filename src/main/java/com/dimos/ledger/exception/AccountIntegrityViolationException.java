package com.dimos.ledger.exception;

public class AccountIntegrityViolationException extends RuntimeException {
    public AccountIntegrityViolationException(String accountReference) {
        super("Checksum integrity violation detected on account: " + accountReference);
    }
}
