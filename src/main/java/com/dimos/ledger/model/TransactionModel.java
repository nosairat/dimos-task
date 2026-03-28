package com.dimos.ledger.model;

import com.dimos.ledger.entity.Entry;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class TransactionModel {
    public TransactionModel(Transaction transaction, List<Entry> entries){
        this.correlationId = transaction.getCorrelationId();
        this.transactionReference = transaction.getTransactionReference();
        this.senderAccountReference = transaction.getSenderAccount().getAccountReference();
        this.receiverAccountReference = transaction.getReceiverAccount().getAccountReference();
        this.amount = transaction.getAmount();
        this.currency = transaction.getCurrency().getCode();
        this.type = transaction.getType();
        this.status = transaction.getStatus();
        this.createdAt = transaction.getCreatedAt();
        this.entries = entries.stream().map(entry -> TransactionEntryModel.builder()
                .accountReference(entry.getAccount().getAccountReference())
                .amount(entry.getAmount())
                .updatedBalance(entry.getUpdatedBalance())
                .build()).toList();
    }
    private String correlationId;
    private String transactionReference;
    private String senderAccountReference;
    private String receiverAccountReference;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;

    private LocalDateTime createdAt;

    List<TransactionEntryModel> entries;
}
