package com.dimos.ledger.security;

import com.dimos.ledger.entity.Account;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountsPrePersistListener {
    final ChecksumService checksumService;
    @PrePersist
    @PreUpdate
    public void setChecksum(Object entity) {
        if (entity instanceof Account account) {
            account.setChecksum(checksumService.compute(account));
        }
    }
    @PostLoad
    public void validateChecksum(Object entity) {
        if (entity instanceof Account account) {
            checksumService.verify(account, account.getChecksum());
        }
    }
}
