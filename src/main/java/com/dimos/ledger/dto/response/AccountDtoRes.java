package com.dimos.ledger.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDtoRes {
    private String accountReference;
    private String userId;
    private String currencyCode;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
