package com.dimos.ledger.controller;

import com.dimos.ledger.dto.request.TransactionHistoryRequest;
import com.dimos.ledger.dto.request.TransactionInquiryRequest;
import com.dimos.ledger.dto.response.TransactionResponse;
import com.dimos.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @Valid @RequestBody TransactionHistoryRequest request) {
        return ResponseEntity.ok(transactionService.getHistory(request));
    }

    @PostMapping("/inquiry")
    public ResponseEntity<TransactionResponse> inquiry(
            @Valid @RequestBody TransactionInquiryRequest request) {
        return ResponseEntity.ok(transactionService.inquiry(request));
    }
}
