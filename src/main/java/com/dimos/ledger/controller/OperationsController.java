package com.dimos.ledger.controller;

import com.dimos.ledger.dto.request.TransferRequest;
import com.dimos.ledger.dto.response.TransferResponse;
import com.dimos.ledger.service.OperationService;
import com.dimos.ledger.service.processor.TransferProcessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final OperationService operationService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operationService.transfer(request));
    }
}
