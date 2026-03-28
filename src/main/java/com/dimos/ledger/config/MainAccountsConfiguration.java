package com.dimos.ledger.config;

import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.CurrencyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MainAccountsConfiguration {
    final AccountRepository accountRepository;
    final CurrencyRepository currencyRepository;
    @PostConstruct
    void createMainAccounts() {
        // This method can be used to initialize main accounts for demo purposes;
        var accountOtp = accountRepository.findByAccountReference("Main-SYP-Topup-account");
        if(accountOtp.isPresent()) return;

        var sypCurrency = currencyRepository.findByCode("SYP").orElseThrow();
        log.debug("Creating main accounts for demo purposes");
        var account = Account.builder()
                .accountReference("Main-SYP-Topup-account")
                .userId("system")
                .currency(sypCurrency)
                .balance(new BigDecimal(1000000))
                .build();
        accountRepository.save(account);
    }
}
