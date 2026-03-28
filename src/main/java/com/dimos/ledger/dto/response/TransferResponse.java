package com.dimos.ledger.dto.response;

import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.model.TransactionModel;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    TransactionModel transaction;
}
