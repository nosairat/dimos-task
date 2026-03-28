package com.dimos.ledger.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionInquiryRequest {

    private String transactionReference;
    private String correlationId;

    @AssertTrue(message = "Either transactionReference or correlationId must be provided")
    public boolean isAtLeastOnePresent() {
        return transactionReference != null || (correlationId != null && !correlationId.isBlank());
    }
}
