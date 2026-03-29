package com.dimos.ledger.controller;

import com.dimos.ledger.dto.request.CreateAccountDtoReq;
import com.dimos.ledger.dto.response.AccountDtoRes;
import com.dimos.ledger.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountDtoRes> createAccount(@Valid @RequestBody CreateAccountDtoReq request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDtoRes> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountDtoRes>> getAccountsByUserId(@RequestParam String userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }
}
