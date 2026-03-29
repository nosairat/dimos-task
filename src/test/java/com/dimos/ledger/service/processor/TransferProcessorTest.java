package com.dimos.ledger.service.processor;

import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.entity.Entry;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import com.dimos.ledger.model.TransferRequest;
import com.dimos.ledger.model.TransactionModel;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.EntryRepository;
import com.dimos.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferProcessorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private TransferProcessor transferProcessor;

    private Currency currency;
    private Account sender;
    private Account receiver;

    @BeforeEach
    void setUp() {
        currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();

        sender = Account.builder()
                .id(1L)
                .accountReference("ACC-SENDER01")
                .currency(currency)
                .balance(new BigDecimal("1000.0000"))
                .build();

        receiver = Account.builder()
                .id(2L)
                .accountReference("ACC-RECV0001")
                .currency(currency)
                .balance(new BigDecimal("0.0000"))
                .build();
    }

    @Test
    void process_happyPath_returnsTransactionModelAndUpdatesBalances() {
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("500.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, receiver));
        when(accountRepository.findAllByIdInWithLockOrderById(anyList())).thenReturn(List.of(sender, receiver));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        TransactionModel result = transferProcessor.process(request);

        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo("corr-001");
        assertThat(result.getSenderAccountReference()).isEqualTo("ACC-SENDER01");
        assertThat(result.getReceiverAccountReference()).isEqualTo("ACC-RECV0001");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getCurrency()).isEqualTo("SYP");

        // Verify balances were updated
        assertThat(sender.getBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(receiver.getBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));
    }

    @Test
    void process_happyPath_createsDoubleEntryRecords() {
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, receiver));
        when(accountRepository.findAllByIdInWithLockOrderById(anyList())).thenReturn(List.of(sender, receiver));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        transferProcessor.process(request);

        ArgumentCaptor<List<Entry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(entryRepository).saveAll(entriesCaptor.capture());

        List<Entry> entries = entriesCaptor.getValue();
        assertThat(entries).hasSize(2);

        Entry debit = entries.stream()
                .filter(e -> e.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .findFirst().orElseThrow();
        Entry credit = entries.stream()
                .filter(e -> e.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst().orElseThrow();

        assertThat(debit.getAmount()).isEqualByComparingTo(new BigDecimal("-100.0000"));
        assertThat(credit.getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(debit.getAccount().getAccountReference()).isEqualTo("ACC-SENDER01");
        assertThat(credit.getAccount().getAccountReference()).isEqualTo("ACC-RECV0001");
    }

    @Test
    void process_duplicateCorrelationId_throwsDimosException() {
        TransferRequest request = buildRequest("duplicate-corr", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId("duplicate-corr")).thenReturn(true);

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.DUPLICATE_CORRELATION_ID));

        verify(accountRepository, never()).findByAccountReferenceIn(anyList());
    }

    @Test
    void process_senderSameAsReceiver_throwsDimosException() {
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-SENDER01", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.SENDER_IS_SAME_AS_RECEIVER));

        verify(accountRepository, never()).findByAccountReferenceIn(anyList());
    }

    @Test
    void process_senderAccountNotFound_throwsDimosException() {
        TransferRequest request = buildRequest("corr-001", "ACC-UNKNOWN", "ACC-RECV0001", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);
        // Only receiver is returned — sender is missing
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(receiver));

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.ACCOUNT_NOT_FOUND));
    }

    @Test
    void process_receiverAccountNotFound_throwsDimosException() {
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-UNKNOWN", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);
        // Only sender is returned — receiver is missing
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender));

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.ACCOUNT_NOT_FOUND));
    }

    @Test
    void process_currencyMismatch_throwsDimosException() {
        Currency usd = Currency.builder().id(2L).code("USD").name("US Dollar").build();
        Account usdReceiver = Account.builder()
                .id(2L)
                .accountReference("ACC-RECV0001")
                .currency(usd)
                .balance(BigDecimal.ZERO)
                .build();

        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("100.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, usdReceiver));

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.CURRENCY_MISMATCH));
    }

    @Test
    void process_insufficientFunds_throwsDimosException() {
        // Sender only has 100, trying to transfer 500
        sender.setBalance(new BigDecimal("100.0000"));
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("500.0000"));

        when(transactionRepository.existsByCorrelationId("corr-001")).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, receiver));
        when(accountRepository.findAllByIdInWithLockOrderById(anyList())).thenReturn(List.of(sender, receiver));

        assertThatThrownBy(() -> transferProcessor.process(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.INSUFFICIENT_FUNDS));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void process_happyPath_transactionTypeIsSetCorrectly() {
        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("100.0000"));
        request.setTransactionType(TransactionType.TRANSFER);

        when(transactionRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, receiver));
        when(accountRepository.findAllByIdInWithLockOrderById(anyList())).thenReturn(List.of(sender, receiver));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        TransactionModel result = transferProcessor.process(request);

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void process_happyPath_updatedBalancesAreSetOnEntries() {
        sender.setBalance(new BigDecimal("1000.0000"));
        receiver.setBalance(new BigDecimal("200.0000"));

        TransferRequest request = buildRequest("corr-001", "ACC-SENDER01", "ACC-RECV0001", new BigDecimal("300.0000"));

        when(transactionRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(accountRepository.findByAccountReferenceIn(anyList())).thenReturn(List.of(sender, receiver));
        when(accountRepository.findAllByIdInWithLockOrderById(anyList())).thenReturn(List.of(sender, receiver));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(entryRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        TransactionModel result = transferProcessor.process(request);

        // Find entry models
        var senderEntry = result.getEntries().stream()
                .filter(e -> e.getAccountReference().equals("ACC-SENDER01"))
                .findFirst().orElseThrow();
        var receiverEntry = result.getEntries().stream()
                .filter(e -> e.getAccountReference().equals("ACC-RECV0001"))
                .findFirst().orElseThrow();

        assertThat(senderEntry.getUpdatedBalance()).isEqualByComparingTo(new BigDecimal("700.0000"));
        assertThat(receiverEntry.getUpdatedBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));
    }

    private TransferRequest buildRequest(String correlationId, String senderRef, String receiverRef, BigDecimal amount) {
        return TransferRequest.builder()
                .correlationId(correlationId)
                .senderAccountReference(senderRef)
                .receiverAccountReference(receiverRef)
                .amount(amount)
                .transactionType(TransactionType.TRANSFER)
                .build();
    }
}
