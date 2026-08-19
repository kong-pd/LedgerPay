package com.ledgerpay.payments.account.api;

import com.ledgerpay.payments.account.application.AccountData;
import com.ledgerpay.payments.account.domain.AccountStatus;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        Long customerId,
        String name,
        AccountStatus status,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static AccountResponse from(AccountData account) {
        return new AccountResponse(
                account.id(),
                account.customerId(),
                account.name(),
                account.status(),
                account.currency(),
                account.createdAt(),
                account.updatedAt()
        );
    }
}
