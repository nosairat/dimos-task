package com.dimos.ledger.exception;

public class DimosException extends RuntimeException {
    private final DimosError dimosError;
    private final String detail;

    public DimosException(DimosError dimosError, String detail) {
        super(dimosError.getMessage() + (detail != null && !detail.isBlank() ? ": " + detail : ""));
        this.dimosError = dimosError;
        this.detail = detail;
    }

    public DimosError getDimosError() {
        return dimosError;
    }

    public String getDetail() {
        return detail;
    }
}
