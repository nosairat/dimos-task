package com.dimos.ledger.service.processor;

import com.dimos.ledger.entity.Account;
import com.dimos.ledger.entity.Entry;
import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.exception.DimosError;
import com.dimos.ledger.exception.DimosException;
import com.dimos.ledger.model.RequestModel;
import com.dimos.ledger.model.TransactionModel;
import com.dimos.ledger.repository.AccountRepository;
import com.dimos.ledger.repository.EntryRepository;
import com.dimos.ledger.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EntryRepository entryRepository;

    @Transactional
    public TransactionModel process(RequestModel request) {

        // Step 1 — Validate correlationId uniqueness& sender is not same as receiver
        if (transactionRepository.existsByCorrelationId(request.getCorrelationId())) {
            throw new DimosException(DimosError.DUPLICATE_CORRELATION_ID, request.getCorrelationId());
        }
        if (request.getSenderAccountReference().equals(request.getReceiverAccountReference())) {
            throw new DimosException(DimosError.SENDER_IS_SAME_AS_RECEIVER, request.getSenderAccountReference());
        }


        // Step 2 — Resolve accounts by reference
        List<Account> accounts= accountRepository.findByAccountReferenceIn(List.of(request.getSenderAccountReference(),request.getReceiverAccountReference()));
        Account sender = accounts.stream()
                .filter(a -> a.getAccountReference().equals(request.getSenderAccountReference()))
                .findFirst()
                .orElseThrow(() -> new DimosException(DimosError.ACCOUNT_NOT_FOUND, request.getSenderAccountReference()));
        Account receiver = accounts.stream()
                .filter(a -> a.getAccountReference().equals(request.getReceiverAccountReference()))
                .findFirst()
                .orElseThrow(() -> new DimosException(DimosError.ACCOUNT_NOT_FOUND, request.getReceiverAccountReference()));
        // Step 3 — Validate same currency
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            throw new DimosException(DimosError.CURRENCY_MISMATCH,
                    sender.getCurrency().getCode() + " vs " + receiver.getCurrency().getCode());
        }


        // Step 5 — Build transaction and entries
        Transaction transaction = Transaction.builder()
                .transactionReference(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(request.getAmount())
                .currency(sender.getCurrency())
                .type(request.getTransactionType())
                .status(TransactionStatus.COMPLETED)
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

        List<Entry> entries= List.of(debitEntry, creditEntry);

        // Step 6 — Acquire write locks ordered by account ID (deadlock prevention)
        accounts = accountRepository.findAllByIdInWithLockOrderById(accounts.stream().map(Account::getId).toList());
        // Step 7 — Apply entries to balances
        accounts.forEach((a)-> {
            updateAccountAndEntry(a, entries);
        });


        // Step 9 — Persist everything
        transactionRepository.save(transaction);
        accountRepository.saveAll(accounts);
        entryRepository.saveAll(entries);

        return new  TransactionModel(transaction,entries);
    }

    private static void updateAccountAndEntry(Account a, List<Entry> entries) {
        Entry entry = entries.stream().filter(e -> e.getAccount().getAccountReference().equals(a.getAccountReference())).findAny().orElseThrow();
        a.setBalance(a.getBalance().add(entry.getAmount()));
        entry.setUpdatedBalance(a.getBalance());

        if(a.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new DimosException(DimosError.INSUFFICIENT_FUNDS, a.getAccountReference());
        }
    }


}
