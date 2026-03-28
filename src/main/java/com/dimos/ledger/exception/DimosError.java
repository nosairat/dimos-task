package com.dimos.ledger.exception;

import lombok.Getter;

public enum DimosError {
    INSUFFICIENT_FUNDS("Insufficient funds for the transaction", "422"),
    CURRENCY_NOT_FOUND("Currency not found", "400"),
    CURRENCY_MISMATCH("Currency mismatch between accounts", "400"),
    ACCOUNT_NOT_FOUND("Account not found", "435"),
    ACCOUNT_INTEGRITY_VIOLATION("Account checksum integrity violation", "417"),
    DUPLICATE_CORRELATION_ID("Duplicate correlationId detected", "409"),
    SENDER_IS_SAME_AS_RECEIVER("Sender account is same as receiver account", "410"),
    TRANSACTION_NOT_FOUND("Transaction not found", "455");

    @Getter
    private final String message;
    @Getter
    private final String code;

    DimosError(String message, String code) {
        this.message = message;
        this.code = code;
    }

    DimosError(String message) {
        this(message, "500");
    }

}
