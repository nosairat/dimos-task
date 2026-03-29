package com.dimos.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDtoReq {

    @NotBlank(message = "correlationId is required")
    private String correlationId;

    @NotBlank(message = "senderAccountReference is required")
    private String senderAccountReference;

    @NotBlank(message = "receiverAccountReference is required")
    private String receiverAccountReference;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
    private BigDecimal amount;
}
