package com.dimos.ledger.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String code) {
        super("Currency not found with code: " + code);
    }
}
