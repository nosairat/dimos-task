package com.dimos.ledger.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String senderCurrency, String receiverCurrency) {
        super("Currency mismatch: sender account currency is " + senderCurrency
                + " but receiver account currency is " + receiverCurrency);
    }
}
