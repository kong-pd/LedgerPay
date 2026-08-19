package com.ledgerpay.payments.customer.application;

import com.ledgerpay.payments.customer.domain.CustomerStatus;

import java.time.LocalDateTime;

public record CustomerData(
        Long id,
        String email,
        String fullName,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}