package com.dimos.ledger.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountReference) {
        super("Insufficient funds in account: " + accountReference);
    }
}
