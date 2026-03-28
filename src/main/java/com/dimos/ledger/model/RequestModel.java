package com.dimos.ledger.model;

import com.dimos.ledger.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
@Builder
public class RequestModel {
    private String correlationId;
    private String senderAccountReference;
    private String receiverAccountReference;
    private BigDecimal amount;
    private TransactionType transactionType;
}
