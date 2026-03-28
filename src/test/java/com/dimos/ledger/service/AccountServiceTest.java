package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.CreateAccountRequest;
import com.dimos.ledger.dto.response.AccountResponse;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.CurrencyRepository;
import com.dimos.ledger.security.ChecksumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ChecksumService checksumService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_validCurrency_returnsAccountResponse() {
        Currency currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-1")
                .currencyCode("SYP")
                .build();

        Account savedAccount = Account.builder()
                .id(1L)
                .userId("user-1")
                .accountReference("ACC-ABCD1234")
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        when(currencyRepository.findByCode("SYP")).thenReturn(Optional.of(currency));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getCurrencyCode()).isEqualTo("SYP");
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getAccountReference()).startsWith("ACC-");
    }

    @Test
    void createAccount_currencyNotFound_throwsDimosException() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-1")
                .currencyCode("USD")
                .build();

        when(currencyRepository.findByCode("USD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.CURRENCY_NOT_FOUND));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void createAccount_savesAccountWithZeroBalance() {
        Currency currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("user-1")
                .currencyCode("SYP")
                .build();

        Account savedAccount = Account.builder()
                .id(1L)
                .userId("user-1")
                .accountReference("ACC-ABCD1234")
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        when(currencyRepository.findByCode("SYP")).thenReturn(Optional.of(currency));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        accountService.createAccount(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());

        Account captured = captor.getValue();
        assertThat(captured.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captured.getUserId()).isEqualTo("user-1");
        assertThat(captured.getCurrency()).isEqualTo(currency);
        assertThat(captured.getAccountReference()).startsWith("ACC-");
    }

    @Test
    void getAccountById_found_returnsAccountResponse() {
        Currency currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();
        Account account = Account.builder()
                .id(1L)
                .userId("user-1")
                .accountReference("ACC-ABCD1234")
                .currency(currency)
                .balance(new BigDecimal("500.0000"))
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getAccountReference()).isEqualTo("ACC-ABCD1234");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getCurrencyCode()).isEqualTo("SYP");
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));
    }

    @Test
    void getAccountById_notFound_throwsDimosException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(99L))
                .isInstanceOf(DimosException.class)
                .satisfies(ex -> assertThat(((DimosException) ex).getDimosError())
                        .isEqualTo(DimosError.ACCOUNT_NOT_FOUND));
    }

    @Test
    void getAccountsByUserId_returnsMappedList() {
        Currency currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();
        Account account1 = Account.builder()
                .id(1L).userId("user-1").accountReference("ACC-00000001")
                .currency(currency).balance(new BigDecimal("100.0000"))
                .createdAt(LocalDateTime.now()).build();
        Account account2 = Account.builder()
                .id(2L).userId("user-1").accountReference("ACC-00000002")
                .currency(currency).balance(new BigDecimal("200.0000"))
                .createdAt(LocalDateTime.now()).build();

        when(accountRepository.findAllByUserId("user-1")).thenReturn(List.of(account1, account2));

        List<AccountResponse> responses = accountService.getAccountsByUserId("user-1");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AccountResponse::getAccountReference)
                .containsExactly("ACC-00000001", "ACC-00000002");
    }

    @Test
    void getAccountsByUserId_noAccounts_returnsEmptyList() {
        when(accountRepository.findAllByUserId("user-unknown")).thenReturn(List.of());

        List<AccountResponse> responses = accountService.getAccountsByUserId("user-unknown");

        assertThat(responses).isEmpty();
    }
}
