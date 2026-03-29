package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.TransferDtoReq;
import com.dimos.ledger.dto.response.TransferDtoRes;
import com.dimos.ledger.entity.enums.TransactionType;
import com.dimos.ledger.model.RequestModel;
import com.dimos.ledger.model.TransactionModel;
import com.dimos.ledger.service.processor.TransferProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private TransferProcessor transferProcessor;

    @InjectMocks
    private OperationService operationService;

    @Test
    void transfer_buildsRequestModelWithCorrectFields() {
        TransferDtoReq request = TransferDtoReq.builder()
                .correlationId("corr-001")
                .senderAccountReference("ACC-SENDER01")
                .receiverAccountReference("ACC-RECV0001")
                .amount(new BigDecimal("250.0000"))
                .build();

        TransactionModel transactionModel = mock(TransactionModel.class);
        when(transferProcessor.process(any(RequestModel.class))).thenReturn(transactionModel);

        operationService.transfer(request);

        ArgumentCaptor<RequestModel> captor = ArgumentCaptor.forClass(RequestModel.class);
        verify(transferProcessor).process(captor.capture());

        RequestModel captured = captor.getValue();
        assertThat(captured.getCorrelationId()).isEqualTo("corr-001");
        assertThat(captured.getSenderAccountReference()).isEqualTo("ACC-SENDER01");
        assertThat(captured.getReceiverAccountReference()).isEqualTo("ACC-RECV0001");
        assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("250.0000"));
        assertThat(captured.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void transfer_returnsTransferResponseWithTransactionModel() {
        TransferDtoReq request = TransferDtoReq.builder()
                .correlationId("corr-001")
                .senderAccountReference("ACC-SENDER01")
                .receiverAccountReference("ACC-RECV0001")
                .amount(new BigDecimal("100.0000"))
                .build();

        TransactionModel transactionModel = mock(TransactionModel.class);
        when(transferProcessor.process(any(RequestModel.class))).thenReturn(transactionModel);

        TransferDtoRes response = operationService.transfer(request);

        assertThat(response).isNotNull();
        assertThat(response.getTransaction()).isSameAs(transactionModel);
    }

    @Test
    void transfer_delegatesToTransferProcessor() {
        TransferDtoReq request = TransferDtoReq.builder()
                .correlationId("corr-001")
                .senderAccountReference("ACC-SENDER01")
                .receiverAccountReference("ACC-RECV0001")
                .amount(new BigDecimal("100.0000"))
                .build();

        when(transferProcessor.process(any(RequestModel.class))).thenReturn(mock(TransactionModel.class));

        operationService.transfer(request);

        verify(transferProcessor, times(1)).process(any(RequestModel.class));
    }
}
