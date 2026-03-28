package com.dimos.ledger.repository;

import com.dimos.ledger.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntryRepository extends JpaRepository<Entry, UUID> {
    List<Entry> findAllByTransactionId(UUID transactionId);
}
