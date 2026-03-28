package com.dimos.ledger.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountReference) {
        super("Account not found with reference: " + accountReference);
    }
}
