package com.dimos.ledger.dto.response;

import com.dimos.ledger.model.TransactionModel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    TransactionModel transaction;
}
