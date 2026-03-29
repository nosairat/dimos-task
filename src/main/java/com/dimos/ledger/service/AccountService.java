package com.dimos.ledger.service;

import com.dimos.ledger.dto.request.CreateAccountDtoReq;
import com.dimos.ledger.dto.response.AccountDtoRes;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
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
    public AccountDtoRes createAccount(CreateAccountDtoReq request) {
        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new DimosException(DimosError.CURRENCY_NOT_FOUND, request.getCurrencyCode()));

        String accountReference = generateAccountReference();
        BigDecimal initialBalance = BigDecimal.ZERO;

        Account account = Account.builder()
                .userId(request.getUserId())
                .accountReference(accountReference)
                .currency(currency)
                .balance(initialBalance)
                .build();

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public AccountDtoRes getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new DimosException(DimosError.ACCOUNT_NOT_FOUND, id.toString()));
        return toResponse(account);
    }

    public List<AccountDtoRes> getAccountsByUserId(String userId) {
        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }



    private String generateAccountReference() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AccountDtoRes toResponse(Account account) {
        return AccountDtoRes.builder()
                .accountReference(account.getAccountReference())
                .userId(account.getUserId())
                .currencyCode(account.getCurrency().getCode())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
