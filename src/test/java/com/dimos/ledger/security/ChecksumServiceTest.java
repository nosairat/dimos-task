package com.dimos.ledger.security;

import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChecksumServiceTest {

    @InjectMocks
    private ChecksumService checksumService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(checksumService, "secret", "test-secret-key");
    }

    @Test
    void compute_returnsNonEmptyHexString() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));

        String checksum = checksumService.compute(account);

        assertThat(checksum).isNotNull().isNotEmpty();
    }

    @Test
    void compute_returnsHmacSha256Length() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));

        String checksum = checksumService.compute(account);

        // HMAC-SHA256 output is 32 bytes = 64 hex characters
        assertThat(checksum).hasSize(64);
    }

    @Test
    void compute_sameInput_returnsSameChecksum() {
        Account account1 = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));
        Account account2 = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));

        String cs1 = checksumService.compute(account1);
        String cs2 = checksumService.compute(account2);

        assertThat(cs1).isEqualTo(cs2);
    }

    @Test
    void compute_differentBalance_returnsDifferentChecksum() {
        Account account1 = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));
        Account account2 = buildAccount("ACC-12345678", new BigDecimal("2000.0000"));

        String cs1 = checksumService.compute(account1);
        String cs2 = checksumService.compute(account2);

        assertThat(cs1).isNotEqualTo(cs2);
    }

    @Test
    void compute_differentAccountReference_returnsDifferentChecksum() {
        Account account1 = buildAccount("ACC-11111111", new BigDecimal("1000.0000"));
        Account account2 = buildAccount("ACC-22222222", new BigDecimal("1000.0000"));

        String cs1 = checksumService.compute(account1);
        String cs2 = checksumService.compute(account2);

        assertThat(cs1).isNotEqualTo(cs2);
    }

    @Test
    void verify_correctChecksum_returnsTrue() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));
        String checksum = checksumService.compute(account);

        boolean result = checksumService.verify(account, checksum);

        assertThat(result).isTrue();
    }

    @Test
    void verify_incorrectChecksum_returnsFalse() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));

        boolean result = checksumService.verify(account, "0000000000000000000000000000000000000000000000000000000000000000");

        assertThat(result).isFalse();
    }

    @Test
    void verify_tamperedBalance_returnsFalse() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));
        String originalChecksum = checksumService.compute(account);

        // Simulate tampering — change balance after checksum was computed
        account.setBalance(new BigDecimal("9999.0000"));

        boolean result = checksumService.verify(account, originalChecksum);

        assertThat(result).isFalse();
    }

    @Test
    void verify_emptyChecksum_returnsFalse() {
        Account account = buildAccount("ACC-12345678", new BigDecimal("1000.0000"));

        boolean result = checksumService.verify(account, "");

        assertThat(result).isFalse();
    }

    private Account buildAccount(String reference, BigDecimal balance) {
        Currency currency = Currency.builder().id(1L).code("SYP").name("Syrian Pound").build();
        return Account.builder()
                .id(1L)
                .accountReference(reference)
                .currency(currency)
                .balance(balance)
                .build();
    }
}
