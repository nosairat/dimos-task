package com.dimos.ledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "currencyCode is required")
    private String currencyCode;
}
