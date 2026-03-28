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
    private UUID accountId;
    private String accountReference;
    private UUID userId;
    private String currencyCode;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
