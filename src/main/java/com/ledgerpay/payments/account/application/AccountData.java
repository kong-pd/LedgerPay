package com.ledgerpay.payments.account.application;

import com.ledgerpay.payments.account.domain.AccountStatus;

import java.time.LocalDateTime;

public record AccountData(
        Long id,
        Long customerId,
        String name,
        AccountStatus status,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}