package com.dimos.ledger.integration;

import com.dimos.ledger.dto.request.CreateAccountRequest;
import com.dimos.ledger.dto.request.TransferRequest;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.CurrencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OperationsControllerIT extends BaseIntegrationTest {

    private static final String TOPUP_ACCOUNT = "Main-SYP-Topup-account";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private String receiverAccountReference;

    @BeforeEach
    void createReceiverAccount() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("receiver-" + UUID.randomUUID())
                .currencyCode("SYP")
                .build();

        String body = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        receiverAccountReference = objectMapper.readTree(body).get("accountReference").asText();
    }

    @Test
    void transfer_happyPath_returns201WithTransactionModel() throws Exception {
        TransferRequest request = buildTransferRequest(TOPUP_ACCOUNT, receiverAccountReference, "100.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.correlationId", is(request.getCorrelationId())))
                .andExpect(jsonPath("$.transaction.senderAccountReference", is(TOPUP_ACCOUNT)))
                .andExpect(jsonPath("$.transaction.receiverAccountReference", is(receiverAccountReference)))
                .andExpect(jsonPath("$.transaction.status", is("COMPLETED")))
                .andExpect(jsonPath("$.transaction.entries", hasSize(2)));
    }

    @Test
    void transfer_happyPath_updatesBalancesCorrectly() throws Exception {
        BigDecimal amount = new BigDecimal("250.00");
        BigDecimal topupBefore = accountRepository
                .findByAccountReference(TOPUP_ACCOUNT).orElseThrow().getBalance();

        TransferRequest request = buildTransferRequest(TOPUP_ACCOUNT, receiverAccountReference, "250.00");
        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        BigDecimal topupAfter = accountRepository
                .findByAccountReference(TOPUP_ACCOUNT).orElseThrow().getBalance();
        BigDecimal receiverBalance = accountRepository
                .findByAccountReference(receiverAccountReference).orElseThrow().getBalance();

        assertThat(topupAfter).isEqualByComparingTo(topupBefore.subtract(amount));
        assertThat(receiverBalance).isEqualByComparingTo(amount);
    }

    @Test
    void transfer_duplicateCorrelationId_returns400() throws Exception {
        TransferRequest request = buildTransferRequest(TOPUP_ACCOUNT, receiverAccountReference, "50.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("DUPLICATE_CORRELATION_ID")));
    }

    @Test
    void transfer_senderSameAsReceiver_returns400() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .senderAccountReference(receiverAccountReference)
                .receiverAccountReference(receiverAccountReference)
                .amount(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("SENDER_IS_SAME_AS_RECEIVER")));
    }

    @Test
    void transfer_insufficientFunds_returns400() throws Exception {
        // receiverAccountReference has zero balance — use it as sender
        TransferRequest request = buildTransferRequest(receiverAccountReference, TOPUP_ACCOUNT, "500.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_FUNDS")));
    }

    @Test
    void transfer_senderAccountNotFound_returns400() throws Exception {
        TransferRequest request = buildTransferRequest("ACC-NONEXISTENT", receiverAccountReference, "100.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("ACCOUNT_NOT_FOUND")));
    }

    @Test
    void transfer_receiverAccountNotFound_returns400() throws Exception {
        TransferRequest request = buildTransferRequest(TOPUP_ACCOUNT, "ACC-NONEXISTENT", "100.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("ACCOUNT_NOT_FOUND")));
    }

    @Test
    void transfer_currencyMismatch_returns400() throws Exception {
        Currency usd = currencyRepository.save(
                Currency.builder()
                        .code("USD" + UUID.randomUUID().toString().substring(0, 3).toUpperCase())
                        .name("US Dollar")
                        .build());
        Account usdAccount = accountRepository.save(Account.builder()
                .accountReference("ACC-USD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId("system")
                .currency(usd)
                .balance(new BigDecimal("5000.00"))
                .build());

        TransferRequest request = buildTransferRequest(TOPUP_ACCOUNT, usdAccount.getAccountReference(), "100.00");

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("CURRENCY_MISMATCH")));
    }

    @Test
    void transfer_missingCorrelationId_returns400WithValidationError() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .senderAccountReference(TOPUP_ACCOUNT)
                .receiverAccountReference(receiverAccountReference)
                .amount(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void transfer_zeroAmount_returns400WithValidationError() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .senderAccountReference(TOPUP_ACCOUNT)
                .receiverAccountReference(receiverAccountReference)
                .amount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/operations/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    private TransferRequest buildTransferRequest(String sender, String receiver, String amount) {
        return TransferRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .senderAccountReference(sender)
                .receiverAccountReference(receiver)
                .amount(new BigDecimal(amount))
                .build();
    }
}
