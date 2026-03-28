package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.CreateAccountRequest;
import com.dimos.ledger.dto.response.AccountResponse;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.exception.AccountIntegrityViolationException;
import com.dimos.ledger.exception.AccountNotFoundException;
import com.dimos.ledger.exception.CurrencyNotFoundException;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.CurrencyRepository;
import com.dimos.ledger.security.ChecksumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final ChecksumService checksumService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new CurrencyNotFoundException(request.getCurrencyCode()));

        String accountReference = generateAccountReference();
        BigDecimal initialBalance = BigDecimal.ZERO;
        String checksum = checksumService.compute(accountReference, initialBalance);

        Account account = Account.builder()
                .userId(request.getUserId())
                .accountReference(accountReference)
                .currency(currency)
                .balance(initialBalance)
                .checksum(checksum)
                .build();

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
        verifyChecksum(account);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        return accountRepository.findAllByUserId(userId)
                .stream()
                .peek(this::verifyChecksum)
                .map(this::toResponse)
                .toList();
    }

    private void verifyChecksum(Account account) {
        if (!checksumService.verify(account.getAccountReference(), account.getBalance(), account.getChecksum())) {
            throw new AccountIntegrityViolationException(account.getAccountReference());
        }
    }

    private String generateAccountReference() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .accountReference(account.getAccountReference())
                .userId(account.getUserId())
                .currencyCode(account.getCurrency().getCode())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
