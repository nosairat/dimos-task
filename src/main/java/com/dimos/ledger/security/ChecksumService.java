package com.dimos.ledger.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class ChecksumService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${ledger.checksum.secret}")
    private String secret;

    public String compute(String accountReference, BigDecimal balance) {
        try {
            String data = accountReference + balance.toPlainString();
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

    public boolean verify(String accountReference, BigDecimal balance, String existingChecksum) {
        String computed = compute(accountReference, balance);
        return computed.equals(existingChecksum);
    }
}
