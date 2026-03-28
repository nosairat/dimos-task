package com.dimos.ledger.service.processor;

import com.dimos.ledger.dto.request.TransferRequest;
import com.dimos.ledger.dto.response.TransferResponse;
import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Entry;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import com.dimos.ledger.exception.*;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.EntryRepository;
import com.dimos.ledger.repository.TransactionRepository;
import com.dimos.ledger.security.ChecksumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EntryRepository entryRepository;
    private final ChecksumService checksumService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResponse process(TransferRequest request) {

        // Step 1 — Validate correlationId uniqueness
        if (transactionRepository.existsByCorrelationId(request.getCorrelationId())) {
            throw new DuplicateCorrelationIdException(request.getCorrelationId());
        }

        // Step 2 — Resolve accounts by reference
        Account sender = accountRepository.findByAccountReference(request.getSenderAccountReference())
                .orElseThrow(() -> new AccountNotFoundException(request.getSenderAccountReference()));

        Account receiver = accountRepository.findByAccountReference(request.getReceiverAccountReference())
                .orElseThrow(() -> new AccountNotFoundException(request.getReceiverAccountReference()));

        // Step 3 — Validate same currency
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            throw new CurrencyMismatchException(
                    sender.getCurrency().getCode(),
                    receiver.getCurrency().getCode()
            );
        }

        // Step 4 — Verify checksums
        verifyChecksum(sender);
        verifyChecksum(receiver);

        // Step 5 — Build transaction and entries
        Transaction transaction = Transaction.builder()
                .transactionReference(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(request.getAmount())
                .currency(sender.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .build();

        Entry debitEntry = Entry.builder()
                .transaction(transaction)
                .account(sender)
                .amount(request.getAmount().negate())
                .build();

        Entry creditEntry = Entry.builder()
                .transaction(transaction)
                .account(receiver)
                .amount(request.getAmount())
                .build();

        // Step 6 — Acquire write locks ordered by account ID (deadlock prevention)
        List<Long> accountIds = List.of(sender.getId(), receiver.getId())
                .stream()
                .sorted()
                .toList();
        accountRepository.findAllByIdInWithLockOrderById(accountIds);

        // Step 7 — Apply entries to balances
        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(request.getAmount()));

        // Step 8 — Check for negative balance
        if (sender.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(request.getSenderAccountReference());
        }

        // Step 9 — Recompute checksums
        sender.setChecksum(checksumService.compute(sender.getAccountReference(), sender.getBalance()));
        receiver.setChecksum(checksumService.compute(receiver.getAccountReference(), receiver.getBalance()));

        // Step 10 — Persist everything
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        entryRepository.save(debitEntry);
        entryRepository.save(creditEntry);
        accountRepository.save(sender);
        accountRepository.save(receiver);

        return TransferResponse.builder()
                .transactionReference(transaction.getTransactionReference())
                .correlationId(transaction.getCorrelationId())
                .senderAccountReference(sender.getAccountReference())
                .receiverAccountReference(receiver.getAccountReference())
                .amount(transaction.getAmount())
                .currency(sender.getCurrency().getCode())
                .status(transaction.getStatus())
                .build();
    }

    private void verifyChecksum(Account account) {
        if (!checksumService.verify(account.getAccountReference(), account.getBalance(), account.getChecksum())) {
            throw new AccountIntegrityViolationException(account.getAccountReference());
        }
    }
}
