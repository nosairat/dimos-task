package com.dimos.ledger.repository;

import com.dimos.ledger.entity.Transaction;
import com.dimos.ledger.entity.enums.TransactionStatus;
import com.dimos.ledger.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByCorrelationId(String correlationId);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByCorrelationId(String correlationId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.senderAccount.accountReference = :accountReference
                OR t.receiverAccount.accountReference = :accountReference)
            AND (:dateFrom IS NULL OR t.createdAt >= :dateFrom)
            AND (:dateTo IS NULL OR t.createdAt <= :dateTo)
            AND (:status IS NULL OR t.status = :status)
            AND (:type IS NULL OR t.type = :type)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findHistory(
            @Param("accountReference") String accountReference,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type
    );
}
