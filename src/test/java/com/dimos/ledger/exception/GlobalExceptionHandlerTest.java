package com.dimos.ledger.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleDimosException_returns400WithErrorDetails() {
        DimosException ex = new DimosException(DimosError.ACCOUNT_NOT_FOUND, "ACC-12345678");

        ResponseEntity<Map<String, Object>> response = handler.handleDimosException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "ACCOUNT_NOT_FOUND");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("message").toString()).contains("ACC-12345678");
    }

    @Test
    void handleDimosException_insufficientFunds_includesDetailInMessage() {
        DimosException ex = new DimosException(DimosError.INSUFFICIENT_FUNDS, "ACC-SENDER01");

        ResponseEntity<Map<String, Object>> response = handler.handleDimosException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "INSUFFICIENT_FUNDS");
        assertThat(response.getBody().get("message").toString()).contains("ACC-SENDER01");
    }

    @Test
    void handleDimosException_duplicateCorrelationId_returnsCorrectError() {
        DimosException ex = new DimosException(DimosError.DUPLICATE_CORRELATION_ID, "corr-001");

        ResponseEntity<Map<String, Object>> response = handler.handleDimosException(ex);

        assertThat(response.getBody()).containsEntry("error", "DUPLICATE_CORRELATION_ID");
        assertThat(response.getBody().get("message").toString()).contains("corr-001");
    }

    @Test
    void handleDimosException_currencyMismatch_returnsCorrectError() {
        DimosException ex = new DimosException(DimosError.CURRENCY_MISMATCH, "SYP vs USD");

        ResponseEntity<Map<String, Object>> response = handler.handleDimosException(ex);

        assertThat(response.getBody()).containsEntry("error", "CURRENCY_MISMATCH");
    }

    @Test
    void handleValidation_returnsBadRequestWithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "userId", "userId is required"));
        bindingResult.addError(new FieldError("request", "currencyCode", "currencyCode is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR");
        assertThat(response.getBody().get("message").toString()).contains("userId");
        assertThat(response.getBody().get("message").toString()).contains("currencyCode");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleValidation_singleFieldError_returnsFieldAndMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "amount must be greater than zero"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getBody().get("message").toString())
                .contains("amount")
                .contains("amount must be greater than zero");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("unexpected database error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "INTERNAL_ERROR");
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleGeneric_doesNotLeakExceptionMessage() {
        Exception ex = new RuntimeException("sensitive internal detail");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getBody().get("message").toString())
                .doesNotContain("sensitive internal detail");
    }
}
