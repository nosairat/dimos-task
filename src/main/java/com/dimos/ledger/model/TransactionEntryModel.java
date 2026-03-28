package com.dimos.ledger.model;

import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@Builder
public class TransactionEntryModel {
    private String accountReference;
    private BigDecimal amount;
    private BigDecimal updatedBalance;

}
