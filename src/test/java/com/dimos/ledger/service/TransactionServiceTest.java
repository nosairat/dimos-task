package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.TransactionHistoryRequest;
import com.dimos.ledger.dto.request.TransactionInquiryRequest;
import com.dimos.ledger.dto.response.TransactionResponse;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import com.dimos.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Currency currency;
    private Account senderAccount;
    private Account receiverAccount;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();

        senderAccount = Account.builder()
                .id(1L).accountReference("ACC-SENDER01").currency(currency)
                .balance(new BigDecimal("500.0000")).build();

        receiverAccount = Account.builder()
                .id(2L).accountReference("ACC-RECV0001").currency(currency)
                .balance(new BigDecimal("100.0000")).build();

        transaction = Transaction.builder()
                .id(1L)
                .transactionReference("TXN-REF-123")
                .correlationId("corr-001")
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .amount(new BigDecimal("200.0000"))
                .currency(currency)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getHistory_returnsListOfTransactionResponses() {
        TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                .accountReference("ACC-SENDER01")
                .build();

        when(transactionRepository.findHistory("ACC-SENDER01", null, null, null, null))
                .thenReturn(List.of(transaction));

        List<TransactionResponse> result = transactionService.getHistory(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionReference()).isEqualTo("TXN-REF-123");
        assertThat(result.get(0).getCorrelationId()).isEqualTo("corr-001");
        assertThat(result.get(0).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void getHistory_withFilters_passesFiltersToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                .accountReference("ACC-SENDER01")
                .dateFrom(from)
                .dateTo(to)
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.TRANSFER)
                .build();

        when(transactionRepository.findHistory("ACC-SENDER01", from, to, TransactionStatus.COMPLETED, TransactionType.TRANSFER))
                .thenReturn(List.of());

        List<TransactionResponse> result = transactionService.getHistory(request);

        assertThat(result).isEmpty();
        verify(transactionRepository).findHistory("ACC-SENDER01", from, to, TransactionStatus.COMPLETED, TransactionType.TRANSFER);
    }

    @Test
    void getHistory_noTransactions_returnsEmptyList() {
        TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                .accountReference("ACC-SENDER01")
                .build();

        when(transactionRepository.findHistory(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        List<TransactionResponse> result = transactionService.getHistory(request);

        assertThat(result).isEmpty();
    }

    @Test
    void inquiry_byTransactionReference_returnsResponse() {
        TransactionInquiryRequest request = TransactionInquiryRequest.builder()
                .transactionReference("TXN-REF-123")
                .build();

        when(transactionRepository.findByTransactionReference("TXN-REF-123"))
                .thenReturn(Optional.of(transaction));

        TransactionResponse result = transactionService.inquiry(request);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionReference()).isEqualTo("TXN-REF-123");
        assertThat(result.getSenderAccountReference()).isEqualTo("ACC-SENDER01");
        assertThat(result.getReceiverAccountReference()).isEqualTo("ACC-RECV0001");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(result.getCurrency()).isEqualTo("SYP");
        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        verify(transactionRepository, never()).findByCorrelationId(any());
    }

    @Test
    void inquiry_byCorrelationId_whenTransactionReferenceIsNull_returnsResponse() {
        TransactionInquiryRequest request = TransactionInquiryRequest.builder()
                .correlationId("corr-001")
                .build();

        when(transactionRepository.findByCorrelationId("corr-001"))
                .thenReturn(Optional.of(transaction));

        TransactionResponse result = transactionService.inquiry(request);

        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo("corr-001");

        verify(transactionRepository, never()).findByTransactionReference(any());
    }

    @Test
    void inquiry_byTransactionReference_notFound_throwsDimosException() {
        TransactionInquiryRequest request = TransactionInquiryRequest.builder()
                .transactionReference("TXN-UNKNOWN")
                .build();

        when(transactionRepository.findByTransactionReference("TXN-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.inquiry(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.TRANSACTION_NOT_FOUND));
    }

    @Test
    void inquiry_byCorrelationId_notFound_throwsDimosException() {
        TransactionInquiryRequest request = TransactionInquiryRequest.builder()
                .correlationId("corr-unknown")
                .build();

        when(transactionRepository.findByCorrelationId("corr-unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.inquiry(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.TRANSACTION_NOT_FOUND));
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        TransactionResponse response = transactionService.toResponse(transaction);

        assertThat(response.getTransactionReference()).isEqualTo(transaction.getTransactionReference());
        assertThat(response.getCorrelationId()).isEqualTo(transaction.getCorrelationId());
        assertThat(response.getSenderAccountReference()).isEqualTo(senderAccount.getAccountReference());
        assertThat(response.getReceiverAccountReference()).isEqualTo(receiverAccount.getAccountReference());
        assertThat(response.getAmount()).isEqualByComparingTo(transaction.getAmount());
        assertThat(response.getCurrency()).isEqualTo(currency.getCode());
        assertThat(response.getType()).isEqualTo(transaction.getType());
        assertThat(response.getStatus()).isEqualTo(transaction.getStatus());
        assertThat(response.getCreatedAt()).isEqualTo(transaction.getCreatedAt());
    }
}
