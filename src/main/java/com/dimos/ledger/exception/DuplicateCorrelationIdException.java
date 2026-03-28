package com.dimos.ledger.exception;

public class DuplicateCorrelationIdException extends RuntimeException {
    public DuplicateCorrelationIdException(String correlationId) {
        super("Duplicate correlationId detected: " + correlationId);
    }
}
