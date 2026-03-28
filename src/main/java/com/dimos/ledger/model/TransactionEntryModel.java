package com.dimos.ledger.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
@Builder
public class TransactionEntryModel {
    private String accountReference;
    private BigDecimal amount;
    private BigDecimal updatedBalance;

}
