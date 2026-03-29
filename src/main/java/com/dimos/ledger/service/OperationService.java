package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.TransferDtoReq;
import com.dimos.ledger.dto.response.TransferDtoRes;
import com.dimos.ledger.entity.enums.TransactionType;
import com.dimos.ledger.model.TransferRequest;
import com.dimos.ledger.service.processor.TransferProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j@RequiredArgsConstructor
public class OperationService {
    final TransferProcessor transferProcessor;

    public TransferDtoRes transfer(TransferDtoReq request) {
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountReference(request.getSenderAccountReference())
                .correlationId(request.getCorrelationId())
                .receiverAccountReference((request.getReceiverAccountReference()))
                .amount(request.getAmount())
                .transactionType(TransactionType.TRANSFER)
                .build();
        var res=  transferProcessor.process(transferRequest);

        return TransferDtoRes.builder()
                .transaction(res)
                .build();

    }
}
