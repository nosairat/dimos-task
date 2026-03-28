package com.dimos.ledger.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private String accountReference;
    private String userId;
    private String currencyCode;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
