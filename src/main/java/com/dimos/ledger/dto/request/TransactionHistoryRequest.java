package com.dimos.ledger.dto.request;

import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryRequest {

    @NotBlank(message = "accountReference is required")
    private String accountReference;

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private TransactionStatus status;
    private TransactionType type;
}
