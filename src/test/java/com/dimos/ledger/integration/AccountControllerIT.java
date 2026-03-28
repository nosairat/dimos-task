package com.dimos.ledger.integration;

import com.dimos.ledger.dto.request.CreateAccountRequest;
import com.dimos.ledger.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerIT extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void createAccount_validRequest_returns201WithAccountResponse() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-" + UUID.randomUUID())
                .currencyCode("SYP")
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountReference", startsWith("ACC-")))
                .andExpect(jsonPath("$.currencyCode", is("SYP")))
                .andExpect(jsonPath("$.balance", is(0)))
                .andExpect(jsonPath("$.userId", is(request.getUserId())));
    }

    @Test
    void createAccount_persistsAccountInDatabase() throws Exception {
        String userId = "user-" + UUID.randomUUID();
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId(userId).currencyCode("SYP").build();

        String responseBody = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String reference = objectMapper.readTree(responseBody).get("accountReference").asText();
        assertThat(accountRepository.findByAccountReference(reference)).isPresent();
    }

    @Test
    void createAccount_unknownCurrency_returns400WithError() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-" + UUID.randomUUID())
                .currencyCode("XYZ")
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("CURRENCY_NOT_FOUND")));
    }

    @Test
    void createAccount_missingCurrencyCode_returns400WithValidationError() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-" + UUID.randomUUID())
                .currencyCode("")
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void getAccountById_existingAccount_returns200() throws Exception {
        String userId = "user-" + UUID.randomUUID();
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .userId(userId).currencyCode("SYP").build();

        String responseBody = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        String reference = objectMapper.readTree(responseBody).get("accountReference").asText();
        Long id = accountRepository.findByAccountReference(reference).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountReference", is(reference)))
                .andExpect(jsonPath("$.userId", is(userId)));
    }

    @Test
    void getAccountById_notFound_returns400WithError() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("ACCOUNT_NOT_FOUND")));
    }

    @Test
    void getAccountsByUserId_returnsAllAccountsForUser() throws Exception {
        String userId = "user-" + UUID.randomUUID();
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId(userId).currencyCode("SYP").build();

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/v1/accounts").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAccountsByUserId_noAccounts_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").param("userId", "user-nonexistent-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
