package com.dimos.ledger.dto.response;

import com.dimos.ledger.entity.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private UUID transactionReference;
    private String correlationId;
    private String senderAccountReference;
    private String receiverAccountReference;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
}
