package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.TransactionHistoryRequest;
import com.dimos.ledger.dto.request.TransactionInquiryRequest;
import com.dimos.ledger.dto.response.TransactionResponse;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import com.dimos.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(TransactionHistoryRequest request) {
        return transactionRepository.findHistory(
                request.getAccountReference(),
                request.getDateFrom(),
                request.getDateTo(),
                request.getStatus(),
                request.getType()
        ).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse inquiry(TransactionInquiryRequest request) {
        Transaction transaction;

        if (request.getTransactionReference() != null) {
            transaction = transactionRepository.findByTransactionReference(request.getTransactionReference())
                    .orElseThrow(() -> new DimosException(DimosError.TRANSACTION_NOT_FOUND,
                            request.getTransactionReference().toString()));
        } else {
            transaction = transactionRepository.findByCorrelationId(request.getCorrelationId())
                    .orElseThrow(() -> new DimosException(DimosError.TRANSACTION_NOT_FOUND,
                            request.getCorrelationId()));
        }

        return toResponse(transaction);
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionReference(transaction.getTransactionReference())
                .correlationId(transaction.getCorrelationId())
                .senderAccountReference(transaction.getSenderAccount().getAccountReference())
                .receiverAccountReference(transaction.getReceiverAccount().getAccountReference())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency().getCode())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
