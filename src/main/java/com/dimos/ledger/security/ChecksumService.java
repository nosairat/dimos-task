package com.dimos.ledger.security;

import com.dimos.ledger.entity.Account;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
@Slf4j
public class ChecksumService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${ledger.checksum.secret}")
    private String secret;

    public String compute(Account account) {
        try {
            String data = getData(account);
            log.debug("Computing checksum for data: {}", data);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute checksum", e);
        }
    }

    private static String getData(Account account) {
        String data = account.getAccountReference() + account.getBalance().toPlainString();
        return data;
    }

    public boolean verify(Account account, String existingChecksum) {

        String computed = compute(account);
        log.debug("Verifying checksum for account {}: computed={}, existing={}", account.getAccountReference(), computed, existingChecksum);
        return computed.equals(existingChecksum);
    }
//    private void verifyChecksum(Account account) {
//        if (!checksumService.verify(account.getAccountReference(), account.getBalance(), account.getChecksum())) {
//            throw new DimosException(DimosError.ACCOUNT_INTEGRITY_VIOLATION, account.getAccountReference());
//        }
//    }
}
